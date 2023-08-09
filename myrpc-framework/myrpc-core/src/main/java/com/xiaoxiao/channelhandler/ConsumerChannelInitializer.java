package com.xiaoxiao.channelhandler;

import com.xiaoxiao.channelhandler.handler.MySimpleChannelInboundHandler;
import com.xiaoxiao.channelhandler.handler.MyrpcRequestEncoder;
import com.xiaoxiao.channelhandler.handler.MyrpcResponseDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast(new LoggingHandler(LogLevel.DEBUG))    // Netty自带的日志处理器
                .addLast(new MyrpcRequestEncoder())// 消息编码器
                .addLast(new MyrpcResponseDecoder())
                .addLast(new MySimpleChannelInboundHandler());
    }
}
