package com.xiaoxiao;

import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.discovery.RegistryConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyrpcBootstrap {

    private static final MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    // 定义相关的一些基础配置
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;
    // 注册中心
    private Registry registry;
    // 维护已经发布且暴露的服务列表 key-> interface的全限定名，value-> ServiceConfig
    private static final Map<String, ServiceConfig<?>> SERVICE_LIST = new ConcurrentHashMap<>(16);
    // 连接的缓存
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    // 全局的对外挂起的 completableFuture
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    // 维护的zookeeper实例
//    private ZooKeeper zooKeeper;

    private MyrpcBootstrap() {

    }

    public static MyrpcBootstrap getInstance() {
        return myrpcBootstrap;
    }

    /**
     * 定义当前应用的名称
     * @param appName 应用名称
     * @return this当前实例
     */
    public MyrpcBootstrap application(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * 配置注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public MyrpcBootstrap registry(RegistryConfig registryConfig) {

        // 维护的zookeeper实例
//        this.zooKeeper = ZookeeperUtils.createZookeeper();

        // 使用 registryConfig 获取注册中心
        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 封装的协议
     * @return this当前实例
     */
    public MyrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;

        if (log.isDebugEnabled()) {
            log.debug("当前工程使用了：{} 协议进行序列化", protocolConfig.toString());
        }
        return this;
    }

    // ---------------------服务提供方的相关api---------------------------

    /**
     * 发布服务,将接口与其匹配的实现注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public MyrpcBootstrap publish(ServiceConfig<?> service) {

        registry.register(service);

        // 当服务调用方，通过接口、方法名、具体的方法参数列表发起调用,为了维护，将其保存起来
        SERVICE_LIST.put(service.getInterface().getName(),service);

        return this;
    }

    /**
     * 批量发布
     * @param services 封装的需要发布的服务集合
     * @return this当前实例
     */
    public MyrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
        // 1、创建eventLoop，boss只负责处理请求，并将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);


        try {
            // 2、服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                                    ByteBuf byteBuf = (ByteBuf) msg;
                                    log.info("byteBuf-->{}", byteBuf.toString(Charset.defaultCharset()));

                                    channelHandlerContext.channel().writeAndFlush(Unpooled.copiedBuffer("myrpc--hello".getBytes()));
                                }
                            });
                        }
                    });
            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    // ----------------服务调用方的相关api-------------------

    /**
     *
     * @param reference
     */
    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 配置reference
        reference.setRegistry(registry);

        return this;
    }
}
