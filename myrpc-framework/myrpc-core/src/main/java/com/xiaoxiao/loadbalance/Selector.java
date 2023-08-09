package com.xiaoxiao.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

public interface Selector {

    /**
     * 根据服务列表执行算法获取一个服务节点
     * @return 具体的服务节点
     */
    InetSocketAddress getNext();

    // Todo 服务动态上下线需要进行reBalance()
    void reBalance();
}
