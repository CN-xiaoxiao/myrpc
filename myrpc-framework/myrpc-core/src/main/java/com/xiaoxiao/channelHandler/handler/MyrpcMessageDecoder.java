package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.transport.message.MessageFormatConstant;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * 解码器
 */
@Slf4j
public class MyrpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public MyrpcMessageDecoder() {
        super(
                MessageFormatConstant.MAX_FRAME_LENGTH,
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                MessageFormatConstant.FULL_FIELD_LENGTH,
                -( MessageFormatConstant.MAGIC.length
                        + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH
                        + MessageFormatConstant.FULL_FIELD_LENGTH ),
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode =  super.decode(ctx, in);

        if (decode instanceof ByteBuf byteBuf) {
            return decodeFrame(byteBuf);
        }

        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        // 1、解析魔数值
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);

        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("获得的请求不合法");
            }
        }
        
        // 2、解析版本号
        byte version = byteBuf.readByte();
        if (version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("获得的请求版本不支持");
        }
        
        // 3、解析头部的长度
        short headLength = byteBuf.readShort();

        // 4、解析总长度
        int fullLength = byteBuf.readInt();

        // 5、请求类型
        byte requestType = byteBuf.readByte();

        // 6、序列化类型
        byte serializeType = byteBuf.readByte();

        // 7、压缩类型
        byte compressType = byteBuf.readByte();

        // 8、请求id
        long requestId = byteBuf.readLong();

        MyrpcRequest myrpcRequest = new MyrpcRequest();
        myrpcRequest.setRequestType(requestType);
        myrpcRequest.setCompressType(compressType);
        myrpcRequest.setSerializeType(serializeType);

        // 判断是否时心跳检测，如果是直接返回
        if (requestType == RequestType.heart_BEAT.getId()) {
            return myrpcRequest;
        }

        // 9、获取负载
        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];

        byteBuf.readBytes(payload);

        // Todo 1、解压缩


        // Todo 2、反序列化
        try(ByteArrayInputStream bais = new ByteArrayInputStream(payload);
            ObjectInputStream ois = new ObjectInputStream(bais)) {

            RequestPayload requestPayload = (RequestPayload) ois.readObject();
            myrpcRequest.setRequestPayload(requestPayload);

        } catch (IOException | ClassNotFoundException e) {
            log.error("请求【{}】反序列化时发生异常",requestId,e);
        }

        return myrpcRequest;
    }
}
