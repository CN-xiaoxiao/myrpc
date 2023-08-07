package com.xiaoxiao;

import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.exceptions.NetWorkException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ReferenceConfig<T> {
    private Class<T> interfaceRef;
    // 注册中心
    private Registry registry;

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    /**
     *
     * @return 返回代理对象
     */
    public T get() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceRef};

        // 使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                // 1、发现服务，从注册中心寻找一个可用的服务
                // Todo 1、不需要每次调用相关方法的时候都需要去注册中心去拉取服务列表
                //      2、选择合理的服务去调用，而不是第一个
                InetSocketAddress address = registry.lookup(interfaceRef.getName());

                if (log.isDebugEnabled()) {
                    log.debug("服务调用方，发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
                }

                // 2、使用netty连接服务器，发送调用的服务的名字+方法名字+参数列表，得到结果
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

                    channel = channelFuture.get(3, TimeUnit.SECONDS);

                    // 缓存channel
                    MyrpcBootstrap.CHANNEL_CACHE.put(address,channel);
                }

                if (channel == null) {
                    log.error("获取或建立与【{}】的通道时发生了异常。",address);
                    throw new NetWorkException("获取通道时发生了异常。");
                }

                // Todo 封装报文


                // 异步策略读取返回结果
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // Todo 将completableFuture 暴露出去
                MyrpcBootstrap.PENDING_REQUEST.put(1L,completableFuture);

                channel.writeAndFlush(Unpooled.copiedBuffer("hello".getBytes()))
                        .addListener( (ChannelFutureListener) promise -> {
                            if (!promise.isSuccess()) {
                                completableFuture.completeExceptionally(promise.cause());
                            }
                        });

//               Object O = completableFuture.get(3, TimeUnit.SECONDS);
                return completableFuture.get(10, TimeUnit.SECONDS);
            }
        });

        return (T) helloProxy;
    }

    public Class<T> getInterfaceRef() {
        return interfaceRef;
    }

    public void setInterfaceRef(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
