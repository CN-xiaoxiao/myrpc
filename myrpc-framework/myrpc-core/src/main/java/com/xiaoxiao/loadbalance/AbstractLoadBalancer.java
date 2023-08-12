package com.xiaoxiao.loadbalance;

import com.xiaoxiao.MyrpcBootstrap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoadBalancer implements LoadBalancer{
    private Map<String, Selector> cache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName, String group) {
        Selector selector = cache.get(serviceName);

        if (selector == null) {
            List<InetSocketAddress> serviceList = MyrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry().lookup(serviceName, group);

            selector = getSelector(serviceList);

            cache.put(serviceName, selector);
        }

        return selector.getNext();
    }

    protected abstract Selector getSelector(List<InetSocketAddress> serviceList);

    @Override
    public synchronized void reLoadBalance(String serviceName, List<InetSocketAddress> addresses) {
        cache.put(serviceName, getSelector(addresses));
    }
}
