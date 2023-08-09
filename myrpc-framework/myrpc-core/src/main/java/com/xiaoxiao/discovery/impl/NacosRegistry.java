package com.xiaoxiao.discovery.impl;

import com.xiaoxiao.ServiceConfig;
import com.xiaoxiao.discovery.AbstractRegistry;

import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class NacosRegistry extends AbstractRegistry {

    public NacosRegistry() {

    }

    public NacosRegistry(String connectString, int timeout) {

    }


    @Override
    public void register(ServiceConfig<?> serviceConfig) {

    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        return null;
    }
}
