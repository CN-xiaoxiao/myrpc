package com.xiaoxiao.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务调用方发起的请求内容
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyrpcRequest {
    // 请求ID
    private long requestId;
    // 请求类型
    private byte requestType;
    // 压缩类型
    private byte compressType;
    // 序列化方式
    private byte serializeType;
    // 时间戳
    private long timeStamp;

    // 具体的消息体
    private RequestPayload requestPayload;
}
