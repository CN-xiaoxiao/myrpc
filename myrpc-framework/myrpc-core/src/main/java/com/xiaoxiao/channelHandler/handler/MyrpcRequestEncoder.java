package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.serialize.Serializer;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MessageFormatConstant;
import com.xiaoxiao.transport.message.MyrpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

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
public class MyrpcRequestEncoder extends MessageToByteEncoder<MyrpcRequest> {
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

        // 序列化
        Serializer serializer = SerializerFactory.getSerializer(MyrpcBootstrap.SERIALIZE_TYPE).getSerializer();

        byte[] body = serializer.serialize(myrpcRequest.getRequestPayload());

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
            log.debug("请求【{}】已经完成报文的编码", myrpcRequest.getRequestId());
        }
    }

}
