package com.xiaoxiao.serialize.impl;


import com.xiaoxiao.serialize.Serializer;
import io.fury.Fury;
import io.fury.Language;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class FurySerializer implements Serializer {
    // Fury应该在多个对象序列化之间复用，不要每次创建新的Fury实例
    private static Fury FURY = Fury.builder()
            .withLanguage(Language.JAVA)
            .build();

    @Override
    public byte[] serialize(Object object) {

        if (object == null) {
            return null;
        }
        Fury fury = FURY;

        fury.register(object.getClass());
        byte[] bytes = fury.serialize(object);

        if (log.isDebugEnabled()) {
            log.debug("对象【{}】已经完成了序列化操作", object);
        }

        return bytes;

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Fury fury = FURY;
        fury.register(clazz);

        Object ob = fury.deserialize(bytes);

        if (log.isDebugEnabled()) {
            log.debug("类【{}】已经完成了反序列化操作", clazz);
        }

        return (T)ob;

    }

}
