package com.xiaoxiao.compress;

import com.xiaoxiao.compress.impl.GzipCompressor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CompressorFactory {
    private static final ConcurrentHashMap<String, CompressWrapper> COMPRESSOR_CACHE = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Byte, CompressWrapper> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        CompressWrapper gzip = new CompressWrapper((byte) 1, "gzip", new GzipCompressor());

        COMPRESSOR_CACHE.put("gzip", gzip);

        COMPRESSOR_CACHE_CODE.put((byte)1, gzip);

    }

    public static CompressWrapper getCompressor(String compressorType) {
        CompressWrapper compressWrapper = COMPRESSOR_CACHE.get(compressorType);

        if (compressWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您设置的【{}】压缩方式, 已使用默认压缩【{}】", compressorType, "gzip");
            }
            return COMPRESSOR_CACHE.get("gzip");
        }

        return compressWrapper;
    }

    public static CompressWrapper getCompressor(byte compressorCode) {
        CompressWrapper compressWrapper = COMPRESSOR_CACHE_CODE.get(compressorCode);

        if (compressWrapper == null) {
            if (log.isDebugEnabled()) {
                log.debug("未找到您设置的【{}】压缩方式, 已使用默认压缩【{}】", compressorCode, "1");
            }
            return COMPRESSOR_CACHE_CODE.get((byte)1);
        }

        return compressWrapper;
    }
}
