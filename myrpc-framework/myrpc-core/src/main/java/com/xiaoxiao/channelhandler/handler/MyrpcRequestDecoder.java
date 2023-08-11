package com.xiaoxiao.channelhandler.handler;

import com.xiaoxiao.compress.Compressor;
import com.xiaoxiao.compress.CompressorFactory;
import com.xiaoxiao.enumeration.RequestType;
import com.xiaoxiao.serialize.Serializer;
import com.xiaoxiao.serialize.SerializerFactory;
import com.xiaoxiao.transport.message.MessageFormatConstant;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * 解码器
 */
@Slf4j
public class MyrpcRequestDecoder extends LengthFieldBasedFrameDecoder {
    public MyrpcRequestDecoder() {
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

        Thread.sleep(new Random().nextInt(50));

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

        // 9、时间戳
        long timeStamp = byteBuf.readLong();

        MyrpcRequest myrpcRequest = new MyrpcRequest();
        myrpcRequest.setRequestType(requestType);
        myrpcRequest.setCompressType(compressType);
        myrpcRequest.setSerializeType(serializeType);
        myrpcRequest.setRequestId(requestId);
        myrpcRequest.setTimeStamp(timeStamp);

        // 判断是否时心跳检测，如果是直接返回
        if (requestType == RequestType.heart_BEAT.getId()) {
            return myrpcRequest;
        }

        // 9、获取负载
        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];

        byteBuf.readBytes(payload);

        if (payload.length != 0) {

            // 1、解压缩
            Compressor compressor = CompressorFactory.getCompressor(compressType).getImpl();
            payload = compressor.decompress(payload);

            // 2、反序列化
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getImpl();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);

            myrpcRequest.setRequestPayload(requestPayload);

        }

        if (log.isDebugEnabled()) {
            log.debug("请求【{}】已经完成在服务端完成报文的解码工作", myrpcRequest.getRequestId());
        }

        return myrpcRequest;
    }
}
