package com.xiaoxiao.serialize;

import com.xiaoxiao.serialize.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SerializerFactory {
    private static final ConcurrentHashMap<String, SerializerWrapper> SERIALIZER_CACHE = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Byte, SerializerWrapper> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        SerializerWrapper jdk = new SerializerWrapper((byte) 1, "jdk", new JdkSerializer());
        SerializerWrapper json = new SerializerWrapper((byte) 2, "json", new JsonSerializer());
        SerializerWrapper hessian = new SerializerWrapper((byte) 3, "hessian", new HessianSerializer());
        SerializerWrapper kryo = new SerializerWrapper((byte) 4, "kryo", new KryoSerializer());
        SerializerWrapper fury = new SerializerWrapper((byte) 5, "fury", new FurySerializer());

        SERIALIZER_CACHE.put("jdk", jdk);
        SERIALIZER_CACHE.put("json", json);
        SERIALIZER_CACHE.put("hessian", hessian);
        SERIALIZER_CACHE.put("kryo", kryo);
        SERIALIZER_CACHE.put("fury", fury);

        SERIALIZER_CACHE_CODE.put((byte)1, jdk);
        SERIALIZER_CACHE_CODE.put((byte)2, json);
        SERIALIZER_CACHE_CODE.put((byte)3, hessian);
        SERIALIZER_CACHE_CODE.put((byte)4, kryo);
        SERIALIZER_CACHE_CODE.put((byte)5, fury);
    }

    public static SerializerWrapper getSerializer(String serializerType) {

        SerializerWrapper serializerWrapper = SERIALIZER_CACHE.get(serializerType);

        if (serializerWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您配置的【{}】序列化方式，已使用默认方式【{}】", serializerType, "jdk");
            }
            return SERIALIZER_CACHE.get("jdk");
        }

        return serializerWrapper;
    }

    public static SerializerWrapper getSerializer(byte serializerCode) {
        SerializerWrapper serializerWrapper = SERIALIZER_CACHE_CODE.get(serializerCode);

        if (serializerWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您配置的【{}】序列化方式，已使用默认方式【{}】", serializerCode, "1");
            }
            return SERIALIZER_CACHE_CODE.get((byte) 1);
        }

        return serializerWrapper;
    }


}
