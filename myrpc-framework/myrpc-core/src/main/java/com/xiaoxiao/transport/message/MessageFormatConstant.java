package com.xiaoxiao.transport.message;

public class MessageFormatConstant {
    public static final byte[] MAGIC = "myrpc".getBytes();
    public static final byte VERSION = 1;
    // 头部信息的长度
    public static final short HEADER_LENGTH = (byte) (MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);
    public static final short FULL_LENGTH = 4;
    public static final int MAX_FRAME_LENGTH = 1024*1024;

    public static final int VERSION_LENGTH = 1;
    // 头部信息所占用的长度
    public static final int HEADER_FIELD_LENGTH = 2;
    public static final int FULL_FIELD_LENGTH = 4;
}
