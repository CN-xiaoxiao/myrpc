package com.xiaoxiao.channelhandler.handler;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.enumeration.ResponseCode;
import com.xiaoxiao.exceptions.ResponseException;
import com.xiaoxiao.protection.CircuitBreaker;
import com.xiaoxiao.transport.message.MyrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<MyrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyrpcResponse myrpcResponse) throws Exception {

        // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> completableFuture = MyrpcBootstrap.PENDING_REQUEST.get(myrpcResponse.getRequestId());

        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = MyrpcBootstrap.getInstance().getConfiguration().getEveryIpCircuitBreaker();
        CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(socketAddress);


        byte code = myrpcResponse.getCode();

        if (code == ResponseCode.FAIL.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，返回错误的结果，响应码为【{}】", myrpcResponse.getRequestId(), myrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.FAIL.getDesc());
        } else if (code == ResponseCode.RATE_LIMIT.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，被限流，响应码为【{}】", myrpcResponse.getRequestId(), myrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.RATE_LIMIT.getDesc());
        } else if (code == ResponseCode.RESOURCE_NOT_FOUND.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为【{}】的请求，未找到目标资源，响应码为【{}】", myrpcResponse.getRequestId(), myrpcResponse.getCode());
            throw new ResponseException(code, ResponseCode.RESOURCE_NOT_FOUND.getDesc());
        } else if (code == ResponseCode.SUCCESS.getCode()) {
            circuitBreaker.recordRequest();
            // 服务提供方给的结果
            Object returnValue = myrpcResponse.getBody();

            completableFuture.complete(returnValue);
            if (log.isDebugEnabled()) {
                log.debug("已寻找到编号为【{}】的completableFuture，处理响应结果", myrpcResponse.getRequestId());
            }
        } else if (code == ResponseCode.SUCCESS_HEART_BEAT.getCode()) {
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug("已寻找到编号为【{}】的completableFuture，处理心跳请求", myrpcResponse.getRequestId());
            }
        }


    }
}
