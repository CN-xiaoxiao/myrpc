package com.xiaoxiao.proxy.handler;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.NettyBootstrapInitializer;
import com.xiaoxiao.annotation.TryTimes;
import com.xiaoxiao.compress.CompressorFactory;
import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.exceptions.DiscoveryException;
import com.xiaoxiao.exceptions.NetWorkException;
import com.xiaoxiao.exceptions.ResponseException;
import com.xiaoxiao.protection.CircuitBreaker;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 封装了客户端通信的基础逻辑，每个代理对象的远程调用过程都封装在了invoke方法中
 * 1、发现可用服务
 * 2、建立连接
 * 3、发生请求
 * 4、得到结果
 */
@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    // 注册中心
    private final Registry registry;
    // 接口
    private final Class<?> interfaceRef;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 从方法中判断是否需要重试
        TryTimes annotation = method.getAnnotation(TryTimes.class);

        // 默认值为0，不重试
        int tryTimes = 0;
        int intervalTime = 0;

        if (annotation != null) {
            tryTimes = annotation.tryTimes();
            intervalTime = annotation.intervalTime();
        }

        while (true) {

            // 1、封装报文
            RequestPayload requestPayload = RequestPayload.builder()
                    .interfaceName(interfaceRef.getName())
                    .methodName(method.getName())
                    .parametersType(method.getParameterTypes())
                    .parametersValue(args)
                    .returnType(method.getReturnType())
                    .build();

            // 对各种请求id和请求类型进行处理
            MyrpcRequest myrpcRequest = MyrpcRequest.builder()
                    .requestId(MyrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                    .compressType(CompressorFactory.getCompressor(MyrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                    .requestType((RequestType.REQUEST.getId()))
                    .serializeType(SerializerFactory.getSerializer(MyrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                    .timeStamp(new Date().getTime())
                    .requestPayload(requestPayload)
                    .build();

            // 将请求存入本地线程
            MyrpcBootstrap.REQUEST_THREAD_LOCAL.set(myrpcRequest);

            // 2、发现服务，从注册中心拉取服务列表，并通过客户端负载均衡寻找一个可用的服务
            InetSocketAddress address = MyrpcBootstrap.getInstance().getConfiguration().getLoadBalancer().selectServiceAddress(interfaceRef.getName());

            if (log.isDebugEnabled()) {
                log.debug("服务调用方，发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
            }

            // 获取当前地址的熔断器
            Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = MyrpcBootstrap.getInstance().getConfiguration().getEveryIpCircuitBreaker();
            CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(address);
            if (circuitBreaker == null) {
                circuitBreaker = new CircuitBreaker(10,.5F);
                everyIpCircuitBreaker.put(address, circuitBreaker);
            }

            try {

                if (myrpcRequest.getRequestType() != RequestType.heart_BEAT.getId() && circuitBreaker.isBreak()) {
                    // 定期打开熔断器
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MyrpcBootstrap
                                    .getInstance()
                                    .getConfiguration()
                                    .getEveryIpCircuitBreaker()
                                    .get(address)
                                    .reset();
                        }
                    }, 5000);
                    throw new RuntimeException("当前熔断器已经开启，无法发送请求");
                }

                // 使用netty连接服务器，发送调用的服务的名字+方法名字+参数列表，得到结果
                // 3、获取一个可用的channel
                Channel channel = getAvailableChannel(address);

                // 4、异步策略读取返回结果
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // 将completableFuture 暴露出去
                MyrpcBootstrap.PENDING_REQUEST.put(myrpcRequest.getRequestId(), completableFuture);

                channel.writeAndFlush(myrpcRequest)
                        .addListener( (ChannelFutureListener) promise -> {
                            if (!promise.isSuccess()) {
                                completableFuture.completeExceptionally(promise.cause());
                            }
                        });

                // 清理threadLocal
                MyrpcBootstrap.REQUEST_THREAD_LOCAL.remove();

                // 5、获得响应的结果
                Object result = completableFuture.get(10, TimeUnit.SECONDS);
                // 记录成功的请求
                circuitBreaker.recordRequest();

                return result;
            } catch (Exception e) {
                tryTimes--;
                // 记录错误的次数
                circuitBreaker.recordErrorRequest();
                try {
                    Thread.sleep(intervalTime);
                } catch (InterruptedException ex) {
                    log.error("在进行重试时发生异常", ex);
                }
                if (tryTimes<0) {
                    log.error("对方法【{}】进行远程调用时，重试{}次, 依旧不可调用", method.getName(), 3 - tryTimes, e);
                    break;

                }
                log.error("在进行第{}重试时发生异常", 3 - tryTimes, e);
            }
        }
        throw new RuntimeException("对方法【" + method.getName() + "】进行调用得到结果时发生异常");
    }


    private Channel getAvailableChannel(InetSocketAddress address) {

        Channel channel = MyrpcBootstrap.CHANNEL_CACHE.get(address);

        if (channel == null) {
            // 创建channel
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();

            NettyBootstrapInitializer.getBootstrap()
                    .connect(address)
                    .addListener((ChannelFutureListener) promise -> {
                        if (promise.isDone()) {

                            if (log.isDebugEnabled()) {
                                log.debug("已经和【{}】成功建立了连接", address);
                            }

                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    });

            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("获取通道时发生异常。", e);
                throw new DiscoveryException(e);
            }

            // 缓存channel
            MyrpcBootstrap.CHANNEL_CACHE.put(address,channel);
        }

        if (channel == null) {
            log.error("获取或建立与【{}】的通道时发生了异常。",address);
            throw new NetWorkException("获取通道时发生了异常。");
        }

        return channel;
    }
}
