package com.xiaoxiao;

import com.xiaoxiao.utils.zookeeper.ZookeeperNode;
import com.xiaoxiao.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.util.List;

/**
 * 注册中心的管理页面
 */
@Slf4j
public class Application {

    public static void main(String[] args) {

        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();
        // 定义节点和数据
        String basePath = "/xiaoxiao-metadata";
        String providerPath = basePath + "/providers";
        String consumerPath = basePath + "/consumers";

        ZookeeperNode baseNode = new ZookeeperNode("/xiaoxiao-metadata",null);
        ZookeeperNode providerNode = new ZookeeperNode(providerPath,null);
        ZookeeperNode consumerNode = new ZookeeperNode(consumerPath,null);

        // 创建节点
        List.of(baseNode,providerNode,consumerNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        // 关闭连接
        ZookeeperUtils.close(zooKeeper);
    }
}
