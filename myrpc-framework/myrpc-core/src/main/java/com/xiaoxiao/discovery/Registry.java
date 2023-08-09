package com.xiaoxiao.discovery;

import com.xiaoxiao.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 注册中心
 */
public interface Registry {

    /**
     * 注册服务
     * @param serviceConfig 服务配置的内容
     */
    public void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取一个可用的服务
     * @param name 服务的名称
     * @return 服务的地址
     */
    List<InetSocketAddress> lookup(String serviceName);
}
