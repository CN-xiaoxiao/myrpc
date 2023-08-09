package com.xiaoxiao.channelhandler.handler;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.transport.message.MyrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;


@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<MyrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyrpcResponse myrpcResponse) throws Exception {

        // 服务提供方给的结果
        Object returnValue = myrpcResponse.getBody();

        // Todo 对code进行处理
        returnValue = returnValue == null ? new Object() : returnValue;

        // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> completableFuture = MyrpcBootstrap.PENDING_REQUEST.get(myrpcResponse.getRequestId());
        completableFuture.complete(returnValue);

        if (log.isDebugEnabled()) {
            log.debug("已寻找到编号为【{}】的completableFuture，处理响应结果", myrpcResponse.getRequestId());
        }
    }
}
