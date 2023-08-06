package com.xiaoxiao;

import com.xiaoxiao.discovery.RegistryConfig;
import com.xiaoxiao.impl.HelloMyrpcImpl;

public class ProviderApplication {
    public static void main(String[] args) {
        // 服务提供方，注册服务，启动服务
        // 1、封装要发布的服务
        ServiceConfig<HelloMyrpc> service = new ServiceConfig<>();
        service.setInterface(HelloMyrpc.class);
        service.setRef(new HelloMyrpcImpl());
        // 2、定义注册中心

        // 3、通过启动引导程序，启动服务提供方发布服务、启动服务
        MyrpcBootstrap.getInstance()
                .application("first-myrpc-provider")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181")) // 配置注册中心
                .protocol(new ProtocolConfig("jdk"))    // 配置序列化协议
                .publish(service)   // 发布服务
                .start();   // 启动服务
    }
}
