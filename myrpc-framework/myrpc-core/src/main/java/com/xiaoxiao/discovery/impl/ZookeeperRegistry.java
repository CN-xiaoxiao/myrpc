package com.xiaoxiao.discovery.impl;

import com.xiaoxiao.Constant;
import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.ServiceConfig;
import com.xiaoxiao.discovery.AbstractRegistry;
import com.xiaoxiao.exceptions.DiscoveryException;
import com.xiaoxiao.utils.NetUtils;
import com.xiaoxiao.utils.zookeeper.ZookeeperNode;
import com.xiaoxiao.utils.zookeeper.ZookeeperUtils;
import com.xiaoxiao.watch.UpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {


    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {
        // 服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();

        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点
        String node = parentNode + "/" + NetUtils.getIp() + ":" + MyrpcBootstrap.getInstance().getConfiguration().getPort();
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }


        if (log.isDebugEnabled()) {
            log.debug("服务{}，已被注册", service.getInterface().getName());
        }
    }

    /**
     * 从注册中心拉取服务列表
     * @param serviceName 服务的名称
     * @return 服务列表
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        // 1、找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName;

        // 2、从zookeeper中获取其子节点, 192.168.0.1:2151
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, new UpAndDownWatcher());

        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            return new InetSocketAddress(ip,port);
        }).toList();

        if (inetSocketAddresses.size() == 0) {
            throw new DiscoveryException("未发现任何可用的服务主机");
        }

        return inetSocketAddresses;
    }
}
