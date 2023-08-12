package com.xiaoxiao.watch;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.NettyBootstrapInitializer;
import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.loadbalance.LoadBalancer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class UpAndDownWatcher implements Watcher {

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
            if (log.isDebugEnabled()) {
                log.debug("检测到服务【{}】有节点上/下线，将重新拉取服务列表。", watchedEvent.getPath());
            }

            String serviceName = getServiceName(watchedEvent.getPath());

            Registry registry = MyrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            List<InetSocketAddress> addresses = registry.lookup(serviceName, MyrpcBootstrap.getInstance().getConfiguration().getGroup());

            // 处理新增的节点
            for (InetSocketAddress address : addresses) {
                if (!MyrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    MyrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            }
            // 处理下线节点
            for (Map.Entry<InetSocketAddress, Channel> entry : MyrpcBootstrap.CHANNEL_CACHE.entrySet()) {
                if (!addresses.contains(entry.getKey())) {
                    MyrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                }
            }

            // 获得负载均衡器，进行重新的loadBalance
            LoadBalancer loadBalancer = MyrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reLoadBalance(serviceName, addresses);

        }
    }

    private String getServiceName(String path) {
        String[] split = path.split("/");

        return split[split.length-1];
    }
}
