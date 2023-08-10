package com.xiaoxiao.loadbalance.impl;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.loadbalance.AbstractLoadBalancer;
import com.xiaoxiao.loadbalance.Selector;
import com.xiaoxiao.transport.message.MyrpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 轮询的负载均衡
 */
@Slf4j
public class ConsistentHashBalancer extends AbstractLoadBalancer {


    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentHashSelector(serviceList, 128);
    }

    /**
     * 一致性hash的具体实现
     */
    private static class ConsistentHashSelector implements Selector {

        // hash环用来存储服务器节点
        private SortedMap<Integer, InetSocketAddress> circle = new TreeMap<>();
        // 虚拟节点个数
        private int virtualNodes;

        public ConsistentHashSelector(List<InetSocketAddress> serviceList, int virtualNodes) {
            this.virtualNodes = virtualNodes;

            for (InetSocketAddress inetSocketAddress : serviceList) {
                // 把节点加入到hash环中
                addNodeToCircle(inetSocketAddress);
            }
        }

        @Override
        public InetSocketAddress getNext() {

            MyrpcRequest myrpcRequest = MyrpcBootstrap.REQUEST_THREAD_LOCAL.get();

            // 获取请求的特征来选择服务器
            String requestId = Long.toString(myrpcRequest.getRequestId());

            // 对请求的特征做hash
            int hash = hash(requestId);

            // 判断该hash值能否直接落在一个服务器上
            if (!circle.containsKey(hash)) {
                // 寻找最近的一个节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }

            return circle.get(hash);
        }


        /**
         * 将每个节点挂载到hash环上
         * @param inetSocketAddress 节点地址
         */
        private void addNodeToCircle(InetSocketAddress inetSocketAddress) {
            // 为每个节点生成匹配的虚拟节点并进行挂载
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress + "-" + i);
                circle.put(hash, inetSocketAddress);
                if (log.isDebugEnabled()) {
                    log.debug("hash为【{}】的节点已挂载到了hash环上。", hash);
                }
            }
        }

        private void removeNodeToCircle(InetSocketAddress inetSocketAddress) {
            // 为每个节点生成匹配的虚拟节点并进行挂载
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress + "-" + "i");
                circle.remove(hash, inetSocketAddress);
            }
        }

        /**
         * 具体的hash算法 Todo 优化算法，使其均匀分布
         * @param s
         * @return
         */
        private int hash(String s) {
            MessageDigest md;

            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            byte[] digest = md.digest(s.getBytes());

            int res = 0;

            for (int i = 0; i < 4; i++) {
                res <<= 8;

                if (digest[i] < 0) {
                    res = res | (digest[i] & 255);
                } else {
                    res |= digest[i];
                }


            }

            return res;
        }

        private String toBinary(int i) {
            String s = Integer.toBinaryString(i);
            int index = 32 -s.length();

            StringBuffer sb = new StringBuffer();

            for (int j = 0; j < index; j++) {
                sb.append(0);
            }

            return sb.toString();
        }

    }

}
