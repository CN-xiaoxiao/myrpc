package com.xiaoxiao.serialize;

public interface Serializer {

    /**
     * 抽象的用来序列化的方法
     * @param object
     * @return
     */
    byte[] serialize(Object object);

    /**
     * 反序列化的方法
     * @param bytes 待反序列化的字节数组
     * @param clazz
     * @return
     * @param <T>
     */
    // 反序列化
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
