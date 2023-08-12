package com.xiaoxiao;

import com.xiaoxiao.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {

        // 获取代理对象
        ReferenceConfig<HelloMyrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloMyrpc.class);

        // 1、连接注册中心
        // 2、拉取服务列表
        // 3、选择服务并连接
        // 4、发送请求
        MyrpcBootstrap.getInstance()
                .application("first-myprc-consumer")
//                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
//                .serialize("fury")
//                .compress("gzip")
                .reference(reference);

        HelloMyrpc helloMyrpc = reference.get();

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        while (true) {
            for (int i = 0; i < 50; i++) {
                String sayHi = helloMyrpc.sayHi("Hello");
                log.info("sayHi-->{}", sayHi);
            }
        }
    }
}
