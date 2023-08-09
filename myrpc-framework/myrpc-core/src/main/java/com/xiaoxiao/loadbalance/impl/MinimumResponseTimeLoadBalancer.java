package com.xiaoxiao.loadbalance.impl;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.loadbalance.AbstractLoadBalancer;
import com.xiaoxiao.loadbalance.Selector;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector(serviceList);
    }

    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceList) {
        }

        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = MyrpcBootstrap
                    .ANSWERING_TIME_CHANNEL_CACHE
                    .firstEntry();
            if (entry != null) {
                return (InetSocketAddress) entry
                        .getValue()
                        .remoteAddress();
            }

            Channel channel = (Channel) MyrpcBootstrap.CHANNEL_CACHE.values().toArray()[0];


            return (InetSocketAddress) channel.remoteAddress();
        }

        @Override
        public void reBalance() {

        }
    }
}
