package com.xiaoxiao.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

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

    /**
     * 当感知节点发生了动态上下线，需要重新进行负载均衡
     * @param serviceName 服务的名称
     */
    void reLoadBalance(String serviceName, List<InetSocketAddress> addresses);
}
