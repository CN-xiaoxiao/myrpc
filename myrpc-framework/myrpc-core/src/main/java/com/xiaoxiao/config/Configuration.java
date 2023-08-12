package com.xiaoxiao.config;

import com.xiaoxiao.IdGenerator;
import com.xiaoxiao.discovery.RegistryConfig;
import com.xiaoxiao.loadbalance.LoadBalancer;
import com.xiaoxiao.loadbalance.impl.RoundRobinLoadBalancer;
import com.xiaoxiao.protection.CircuitBreaker;
import com.xiaoxiao.protection.RateLimiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 全局的配置类，代码配置--->xml配置--->spi配置--->默认项
 */
@Data
@Slf4j
public class Configuration {
    // 端口号
    private int port = 8090;

    // 应用名称的名字
    private String appName = "default";
    // 注册中心
    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
    private String serializeType = "jdk";
    private String compressType = "gzip";
    // ID生成器
    private IdGenerator idGenerator = new IdGenerator(1,2);
    // 负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    private String packageName;
    // 为每一个ip配置一个限流器
    private final Map<SocketAddress, RateLimiter> everyIpRateLimiter = new ConcurrentHashMap<>(16);
    // 为每一个ip配置一个熔断器
    private final Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = new ConcurrentHashMap<>(16);

    // 读配置信息
    public Configuration() {
        // spi机制发现相关配置
        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadFromSpi(this);

        // 读取xml获取相关配置
        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadFromXml(this);
    }
}
