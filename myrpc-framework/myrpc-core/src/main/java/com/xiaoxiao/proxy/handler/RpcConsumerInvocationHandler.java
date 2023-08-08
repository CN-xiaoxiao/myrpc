package com.xiaoxiao.proxy.handler;

import com.xiaoxiao.IdGenerator;
import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.NettyBootstrapInitializer;
import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.exceptions.DiscoveryException;
import com.xiaoxiao.exceptions.NetWorkException;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
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

        // 1、发现服务，从注册中心寻找一个可用的服务
        InetSocketAddress address = registry.lookup(interfaceRef.getName());

        if (log.isDebugEnabled()) {
            log.debug("服务调用方，发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
        }

        // 2、使用netty连接服务器，发送调用的服务的名字+方法名字+参数列表，得到结果
        Channel channel = getAvailableChannel(address);

        // 封装报文
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parametersType(method.getParameterTypes())
                .parametersValue(args)
                .returnType(method.getReturnType())
                .build();

        // Todo 对各种请求id和请求类型进行处理
        MyrpcRequest myrpcRequest = MyrpcRequest.builder()
                .requestId(MyrpcBootstrap.ID_GENERATOR.getId())
                .compressType((byte) 1)
                .requestType((RequestType.REQUEST.getId()))
                .serializeType(SerializerFactory.getSerializer(MyrpcBootstrap.SERIALIZE_TYPE).getCode())
                .requestPayload(requestPayload)
                .build();

        // 异步策略读取返回结果
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        // 将completableFuture 暴露出去
        MyrpcBootstrap.PENDING_REQUEST.put(1L,completableFuture);

        channel.writeAndFlush(myrpcRequest)
                .addListener( (ChannelFutureListener) promise -> {
                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

        // 5、获得响应的结果
        return completableFuture.get(10, TimeUnit.SECONDS);
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
