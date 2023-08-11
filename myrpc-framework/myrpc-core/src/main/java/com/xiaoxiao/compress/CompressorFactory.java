package com.xiaoxiao.compress;

import com.xiaoxiao.compress.impl.GzipCompressor;
import com.xiaoxiao.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CompressorFactory {
    private static final Map<String, ObjectWrapper<Compressor>> COMPRESSOR_CACHE = new ConcurrentHashMap<>(8);
    private static final Map<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1, "gzip", new GzipCompressor());

        COMPRESSOR_CACHE.put("gzip", gzip);

        COMPRESSOR_CACHE_CODE.put((byte)1, gzip);

    }

    public static ObjectWrapper<Compressor> getCompressor(String compressorType) {
        ObjectWrapper<Compressor> objectWrapper = COMPRESSOR_CACHE.get(compressorType);

        if (objectWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您设置的【{}】压缩方式, 已使用默认压缩【{}】", compressorType, "gzip");
            }
            return COMPRESSOR_CACHE.get("gzip");
        }

        return objectWrapper;
    }

    public static ObjectWrapper<Compressor> getCompressor(byte compressorCode) {
        ObjectWrapper<Compressor> objectWrapper = COMPRESSOR_CACHE_CODE.get(compressorCode);

        if (objectWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您设置的【{}】压缩方式, 已使用默认压缩【{}】", compressorCode, "1");
            }
            return COMPRESSOR_CACHE_CODE.get((byte)1);
        }

        return objectWrapper;
    }

    /**
     * 给工厂添加一个新的压缩策略包装
     * @param objectWrapper
     */
    public static void addCompressor(ObjectWrapper<Compressor> objectWrapper) {
        COMPRESSOR_CACHE.put(objectWrapper.getType(), objectWrapper);
        COMPRESSOR_CACHE_CODE.put(objectWrapper.getCode(), objectWrapper);
    }
}
