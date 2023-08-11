package com.xiaoxiao.serialize;

import com.xiaoxiao.config.ObjectWrapper;
import com.xiaoxiao.serialize.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SerializerFactory {
    private static final Map<String, ObjectWrapper<Serializer>> SERIALIZER_CACHE = new ConcurrentHashMap<>(8);
    private static final Map<Byte, ObjectWrapper<Serializer>> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Serializer> jdk = new ObjectWrapper<>((byte) 1, "jdk", new JdkSerializer());
        ObjectWrapper<Serializer> json = new ObjectWrapper<>((byte) 2, "json", new JsonSerializer());
        ObjectWrapper<Serializer> hessian = new ObjectWrapper<>((byte) 3, "hessian", new HessianSerializer());
        ObjectWrapper<Serializer> kryo = new ObjectWrapper<>((byte) 4, "kryo", new KryoSerializer());
        ObjectWrapper<Serializer> fury = new ObjectWrapper<>((byte) 5, "fury", new FurySerializer());

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

    public static ObjectWrapper<Serializer> getSerializer(String serializerType) {

        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE.get(serializerType);

        if (serializerWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您配置的【{}】序列化方式，已使用默认方式【{}】", serializerType, "jdk");
            }
            return SERIALIZER_CACHE.get("jdk");
        }

        return serializerWrapper;
    }

    public static ObjectWrapper<Serializer> getSerializer(byte serializerCode) {
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE_CODE.get(serializerCode);

        if (serializerWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您配置的【{}】序列化方式，已使用默认方式【{}】", serializerCode, "1");
            }
            return SERIALIZER_CACHE_CODE.get((byte) 1);
        }

        return serializerWrapper;
    }

    /**
     * 添加一个新的序列化方式
     * @param objectWrapper
     */
    public static void addSerializer(ObjectWrapper<Serializer> objectWrapper) {
        SERIALIZER_CACHE.put(objectWrapper.getType(), objectWrapper);
        SERIALIZER_CACHE_CODE.put(objectWrapper.getCode(), objectWrapper);
    }

}
