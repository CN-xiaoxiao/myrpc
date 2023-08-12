package com.xiaoxiao.enumeration;

/**
 * 成功码：20（方法成功调用），21（心跳成功返回）
 * 错误码：50（请求的方法不存在，服务端）， 44（客户端错误）
 * 负载码：31（服务器负载过高，被限流）
 */
public enum ResponseCode {
    SUCCESS((byte) 20, "成功"),
    SUCCESS_HEART_BEAT((byte) 21, "心跳检测成功返回"),
    RATE_LIMIT((byte) 31, "服务被限流"),
    RESOURCE_NOT_FOUND((byte) 44, "请求的资源不存在"),
    FAIL((byte) 50, "方法调用失败"),
    CLOSING((byte) 51, "服务器正在关闭");

    private byte code;
    private String desc;

    ResponseCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
