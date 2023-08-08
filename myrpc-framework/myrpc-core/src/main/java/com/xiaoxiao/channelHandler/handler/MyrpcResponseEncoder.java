package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.serialize.Serializer;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MessageFormatConstant;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.MyrpcResponse;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * 协议编码器
 * 5B magic（魔术值） --- myrpc.getBytes()
 * 1B version（版本）  --- 1
 * 2B header length（首部长度）
 * 4B full length（报文总长度）
 * 1B code
 * 1B serialize type
 * 1B compress type
 * 8B request id
 *
 */
@Slf4j
public class MyrpcResponseEncoder extends MessageToByteEncoder<MyrpcResponse> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyrpcResponse myrpcResponse, ByteBuf byteBuf) throws Exception {

        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);

        byteBuf.writeByte(myrpcResponse.getCode());
        byteBuf.writeByte(myrpcResponse.getSerializeType());
        byteBuf.writeByte(myrpcResponse.getCompressType());

        byteBuf.writeLong(myrpcResponse.getRequestId());

        // Todo 压缩

        // 对响应进行序列化
        Serializer serializer = SerializerFactory
                .getSerializer(myrpcResponse.getSerializeType())
                .getSerializer();
        byte[] body = serializer.serialize(myrpcResponse.getBody());

        if (body!=null) {
            byteBuf.writeBytes(body);
        }

        int bodyLength = body == null ? 0 : body.length;

        // 重新处理报文的总长度
        int writerIndex = byteBuf.writerIndex();
        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                + MessageFormatConstant.HEADER_FIELD_LENGTH);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        // 写指针归位
        byteBuf.writerIndex(writerIndex);

        if (log.isDebugEnabled()) {
            log.debug("响应【{}】已经在服务端完成编码工作", myrpcResponse.getRequestId());
        }

    }
}
