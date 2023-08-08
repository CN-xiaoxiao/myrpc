package com.xiaoxiao.serialize.impl;

import com.alibaba.fastjson2.JSON;
import com.xiaoxiao.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 无法处理.class
 */
@Slf4j
public class JsonSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {

        if (object == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("对象【{}】完成了序列化操作", object);
        }

        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("类【{}】已经完成了反序列化操作", clazz);
        }

        return JSON.parseObject(bytes, clazz);
    }


}
