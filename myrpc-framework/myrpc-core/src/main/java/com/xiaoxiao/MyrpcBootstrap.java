package com.xiaoxiao;

import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
