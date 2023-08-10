package com.xiaoxiao.code;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.NettyBootstrapInitializer;
import com.xiaoxiao.compress.CompressorFactory;
import com.xiaoxiao.discovery.Registry;
import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MyrpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName) {
        // 1、从注册中心拉取服务列表并进行连接
        Registry registry = MyrpcBootstrap.getInstance().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(serviceName);

        // 2、将连接进行缓存
        for (InetSocketAddress address : addresses) {
            try {
                if (!MyrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = NettyBootstrapInitializer.getBootstrap()
                            .connect(address)
                            .sync()
                            .channel();
                    MyrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }


            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 3、定期发送消息
        Thread thread = new Thread(() -> {
            new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000);
        }, "myrpc-HeartbeatDetector-thread");

        thread.setDaemon(true);
        thread.start();
    }

    private static class MyTimerTask extends TimerTask {
        @Override
        public void run() {

            // 将响应时长的map清空
            MyrpcBootstrap.ANSWERING_TIME_CHANNEL_CACHE.clear();

            // 遍历所有的channel
            Map<InetSocketAddress, Channel> channelCache = MyrpcBootstrap.CHANNEL_CACHE;

            for (Map.Entry<InetSocketAddress, Channel> entry : channelCache.entrySet()) {

                int tryTimes = 3;

                while (tryTimes > 0) {
                    Channel channel = entry.getValue();

                    long start = System.currentTimeMillis();

                    // 构建一个心跳请求
                    MyrpcRequest myrpcRequest = MyrpcRequest.builder()
                            .requestId(MyrpcBootstrap.ID_GENERATOR.getId())
                            .compressType(CompressorFactory.getCompressor(MyrpcBootstrap.COMPRESS_TYPE).getCode())
                            .requestType((RequestType.heart_BEAT.getId()))
                            .serializeType(SerializerFactory.getSerializer(MyrpcBootstrap.SERIALIZE_TYPE).getCode())
                            .timeStamp(start)
                            .build();

                    // 异步策略读取返回结果
                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    // 将completableFuture 暴露出去
                    MyrpcBootstrap.PENDING_REQUEST.put(myrpcRequest.getRequestId(), completableFuture);

                    channel.writeAndFlush(myrpcRequest)
                            .addListener( (ChannelFutureListener) promise -> {
                                if (!promise.isSuccess()) {
                                    completableFuture.completeExceptionally(promise.cause());
                                }
                            });

                    Long endTime = 0L;
                    try {
                        completableFuture.get(1, TimeUnit.SECONDS);
                        endTime = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {

                        tryTimes--;

                        log.error("和地址为【{}】的主机连接发生异常,正在进行第【{}】次连接......", channel.remoteAddress(), 3 - tryTimes);

                        if (tryTimes == 0) {
                            // 将失效的地址移出服务列表
                            MyrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                        }

                        try {
                            Thread.sleep(10 * (new Random().nextInt(5)));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }

                        continue;
                    }

                    Long time = endTime - start;

                    MyrpcBootstrap.ANSWERING_TIME_CHANNEL_CACHE.put(time, channel);
                    log.debug( "和【{}】服务器的响应时间是【{}】 " , entry.getKey(), time);
                    break;
                }
            }

            log.info("------------------响应时间treemap-----------------------");
            for (Map.Entry<Long, Channel> entry : MyrpcBootstrap.ANSWERING_TIME_CHANNEL_CACHE.entrySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("【{}】---->【{}】", entry.getKey(), entry.getValue().id());
                }
            }

        }
    }

}
