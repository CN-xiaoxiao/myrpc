package com.xiaoxiao;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MyrpcBootstrap {

    private static MyrpcBootstrap myrpcBootstrap = new MyrpcBootstrap();

    private MyrpcBootstrap() {

    }

    public static MyrpcBootstrap getInstance() {
        return myrpcBootstrap;
    }

    // ---------------------服务提供方的相关api---------------------------
    /**
     * 定义当前应用的名称
     * @param appName 应用名称
     * @return this当前实例
     */
    public MyrpcBootstrap application(String appName) {
        return this;
    }

    /**
     * 配置注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public MyrpcBootstrap registry(RegistryConfig registryConfig) {
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 封装的协议
     * @return this当前实例
     */
    public MyrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        if (log.isDebugEnabled()) {
            log.debug("当前工程使用了：{} 协议进行序列化", protocolConfig.toString());
        }
        return this;
    }

    /**
     * 发布服务
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public MyrpcBootstrap publish(ServiceConfig<?> service) {
        if (log.isDebugEnabled()) {
            log.debug("服务{}，已被注册", service.getInterface().getName());
        }
        return this;
    }

    /**
     * 批量发布
     * @param services 封装的需要发布的服务集合
     * @return this当前实例
     */
    public MyrpcBootstrap publish(List<?> services) {
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
    }


    // ----------------服务调用方的相关api-------------------

    /**
     *
     * @param reference
     */
    public MyrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 配置reference

        return this;
    }
}
