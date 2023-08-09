package com.xiaoxiao.loadbalance;

import java.net.InetSocketAddress;

/**
 * 负载均衡器的接口
 */
public interface LoadBalancer {
    /**
     *  根据服务名获取可用服务
     * @param serviceName 服务名称
     * @return 可用服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName);
}
