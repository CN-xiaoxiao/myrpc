package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.MyrpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        log.info("msg-->{}", msg.toString(Charset.defaultCharset()));
        // 服务提供方给的结果
        String result = msg.toString(Charset.defaultCharset());
        // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> completableFuture = MyrpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(result);
    }
}
