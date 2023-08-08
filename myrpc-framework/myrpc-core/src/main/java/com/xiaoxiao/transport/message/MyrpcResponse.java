package com.xiaoxiao.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务提供方回应的响应内容
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyrpcResponse {
    // 请求ID
    private long requestId;
    // 压缩类型
    private byte compressType;
    // 序列化方式
    private byte serializeType;
    // 响应码 200 成功；500 异常
    private byte code;

    // 具体的消息体
    private Object body;
}
