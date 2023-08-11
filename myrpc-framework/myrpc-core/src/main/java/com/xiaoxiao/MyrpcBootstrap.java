package com.xiaoxiao;

import com.xiaoxiao.annotation.MyrpcApi;
import com.xiaoxiao.channelhandler.handler.MethodCallHandler;
import com.xiaoxiao.channelhandler.handler.MyrpcRequestDecoder;
import com.xiaoxiao.channelhandler.handler.MyrpcResponseEncoder;
import com.xiaoxiao.code.HeartbeatDetector;
import com.xiaoxiao.config.Configuration;
import com.xiaoxiao.discovery.RegistryConfig;
import com.xiaoxiao.loadbalance.LoadBalancer;
import com.xiaoxiao.transport.message.MyrpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MyrpcBootstrap {
    private static final MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    // 全局配置中心
    private Configuration configuration;

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名，value-> ServiceConfig
    public static final Map<String, ServiceConfig<?>> SERVICE_LIST = new ConcurrentHashMap<>(16);
    // 连接的缓存
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public static final TreeMap<Long, Channel> ANSWERING_TIME_CHANNEL_CACHE = new TreeMap<>();
    // 全局的对外挂起的 completableFuture
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);
    // 保存request对象，可用在当前线程内随时获取
    public static final ThreadLocal<MyrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();



    private MyrpcBootstrap() {
        configuration = new Configuration();
    }

    public static MyrpcBootstrap getInstance() {
        return myrpcBootstrap;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 定义当前应用的名称
     * @param appName 应用名称
     * @return this当前实例
     */
    public MyrpcBootstrap application(String appName) {
        configuration.setAppName(appName);
        return this;
    }

    /**
     * 配置注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public MyrpcBootstrap registry(RegistryConfig registryConfig) {
        // 使用 registryConfig 获取注册中心
        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    /**
     * 配置负载均衡策略
     * @param loadBalancer 负载均衡策略
     * @return this当前实例
     */
    public MyrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }

    // ---------------------服务提供方的相关api---------------------------

    /**
     * 发布服务,将接口与其匹配的实现注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public MyrpcBootstrap publish(ServiceConfig<?> service) {

        configuration.getRegistryConfig().getRegistry().register(service);

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
                            socketChannel.pipeline()
                                    .addLast(new LoggingHandler())
                                    .addLast(new MyrpcRequestDecoder())
                                    .addLast(new MethodCallHandler())
                                    .addLast(new MyrpcResponseEncoder());
                        }
                    });
            // 4、绑定端口
            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();

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

    /**
     * 扫描包，进行批量注册
     * @param packageName 包名
     * @return this当前实例
     */
    public MyrpcBootstrap scan(String packageName) {

        // 1、通过packageName获取旗下所有的类的全限定名称
        List<String> classNames = getAllClassNames(packageName);

        // 2、通过反射获取其的接口，构建具体实现
        List<Class<?>> collect = classNames
                .stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(clazz -> clazz.getAnnotation(MyrpcApi.class) != null)
                .collect(Collectors.toList());

        for (Class<?> clazz : collect) {
            // 获取接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);

                if (log.isDebugEnabled()) {
                    log.debug("---->已经通过包扫描将服务【{}】发布", anInterface);
                }

                // 3、发布
                publish(serviceConfig);
            }


        }

        return this;
    }

    public MyrpcBootstrap scan() {

        // 1、通过packageName获取旗下所有的类的全限定名称
        List<String> classNames = getAllClassNames(configuration.getPackageName());

        // 2、通过反射获取其的接口，构建具体实现
        List<Class<?>> collect = classNames
                .stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(clazz -> clazz.getAnnotation(MyrpcApi.class) != null)
                .collect(Collectors.toList());

        for (Class<?> clazz : collect) {
            // 获取接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);

                if (log.isDebugEnabled()) {
                    log.debug("---->已经通过包扫描将服务【{}】发布", anInterface);
                }

                // 3、发布
                publish(serviceConfig);
            }


        }

        return this;
    }

    private List<String> getAllClassNames(String packageName) {
        // 1、通过packageName获取绝对路径
        String basePath = packageName.replaceAll("\\.","/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);

        if (url == null) {
            throw new RuntimeException("包扫描时，发布路径不存在");
        }

        String absolutePath = url.getPath();

        List<String> classNames = new ArrayList<>();

        classNames = recursionFile(absolutePath, classNames, basePath);

        return classNames;
    }

    private List<String> recursionFile(String absolutePath, List<String> classNames, String basePath) {
        File file = new File(absolutePath);

        if (file.isDirectory()) {
            File[] children = file.listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));

            if (children == null || children.length == 0) {
                return classNames;
            }

            for (File child : children) {
                if (child.isDirectory()) {
                    recursionFile(child.getAbsolutePath(), classNames, basePath);
                } else {

                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(), basePath);
                    classNames.add(className);
                }
            }
        } else {
            String className = getClassNameByAbsolutePath(absolutePath, basePath);
            classNames.add(className);
        }

        return classNames;
    }

    private String getClassNameByAbsolutePath(String absolutePath, String basePath) {
        String fileName = absolutePath
                .substring(absolutePath.indexOf(basePath.replaceAll("/", "\\\\")))
                .replaceAll("\\\\", ".");

        fileName = fileName.substring(0, fileName.indexOf(".class"));

        return fileName;
    }


    // ----------------服务调用方的相关api-------------------

    /**
     *
     * @param reference
     */
    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启对此服务的心跳检测
        if (log.isDebugEnabled()) {
            log.debug("开始对服务【{}】的心跳检测。", reference.getInterface().getName());
        }
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());

        // 配置reference
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());

        return this;
    }

    /**
     * 配置序列化的方式
     * @param serializeType
     * @return
     */
    public MyrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);

        if (log.isDebugEnabled()) {
            log.debug("配置了使用的序列化方式为【{}】", serializeType);
        }

        return this;
    }

    /**
     * 配置压缩方式
     * @param compressType
     * @return
     */
    public MyrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);

        if (log.isDebugEnabled()) {
            log.debug("配置了使用的解压缩方式为【{}】", compressType);
        }

        return this;
    }

}
