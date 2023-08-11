package com.xiaoxiao.config;

import com.xiaoxiao.compress.Compressor;
import com.xiaoxiao.compress.CompressorFactory;
import com.xiaoxiao.loadbalance.LoadBalancer;
import com.xiaoxiao.serialize.Serializer;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.spi.SpiHandler;

import java.util.List;

public class SpiResolver {
    public void loadFromSpi(Configuration configuration) {

        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);
        if (loadBalancerWrappers != null && loadBalancerWrappers.size() > 0) {
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }

        List<ObjectWrapper<Compressor>> objectWrappers = SpiHandler.getList(Compressor.class);
        if (objectWrappers  != null) {
            objectWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> list = SpiHandler.getList(Serializer.class);
        if (list != null) {
            list.forEach(SerializerFactory::addSerializer);
        }

    }
}
