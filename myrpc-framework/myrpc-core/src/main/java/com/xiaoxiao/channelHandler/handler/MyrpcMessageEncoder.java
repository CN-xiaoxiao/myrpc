package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.transport.message.MessageFormatConstant;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 协议编码器
 * 5B magic（魔术值） --- myrpc.getBytes()
 * 1B version（版本）  --- 1
 * 2B header length（首部长度）
 * 4B full length（报文总长度）
 * 1B request type
 * 1B serialize type
 * 1B compress type
 * 8B request id
 *
 */
@Slf4j
public class MyrpcMessageEncoder extends MessageToByteEncoder<MyrpcRequest> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyrpcRequest myrpcRequest, ByteBuf byteBuf) throws Exception {

        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);

        byteBuf.writeByte(myrpcRequest.getRequestType());
        byteBuf.writeByte(myrpcRequest.getSerializeType());
        byteBuf.writeByte(myrpcRequest.getCompressType());

        byteBuf.writeLong(myrpcRequest.getRequestId());


        byte[] body = getBodyBytes(myrpcRequest.getRequestPayload());
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
    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {

        if (requestPayload == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            outputStream.writeObject(requestPayload);

            // Todo 压缩

            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化时发生异常。");
            throw new RuntimeException(e);
        }

    }
}
