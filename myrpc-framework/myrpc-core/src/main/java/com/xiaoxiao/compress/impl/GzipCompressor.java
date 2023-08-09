package com.xiaoxiao.compress.impl;

import com.xiaoxiao.compress.Compressor;
import com.xiaoxiao.exceptions.CompressException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 使用GZIP进行压缩
 */
@Slf4j
public class GzipCompressor implements Compressor {
    @Override
    public byte[] compress(byte[] bytes) {

        byte[] result = null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos)) {

            gzipOutputStream.write(bytes);
            gzipOutputStream.finish();
            result = baos.toByteArray();

            if (log.isDebugEnabled()) {
                log.debug("对字节数组进行了压缩，长度由【{}】压缩至【{}】", bytes.length, result.length);
            }

        } catch (IOException e) {
            log.error("对字节数组进行压缩时方式异常，", e);
            throw new CompressException(e);
        }

        return result;
    }

    @Override
    public byte[] decompress(byte[] bytes) {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             GZIPInputStream gzipInputStream = new GZIPInputStream(bais)) {

            byte[] result = gzipInputStream.readAllBytes();

            if (log.isDebugEnabled()) {
                log.debug("对字节数组进行了解压缩，长度由【{}】解压缩至【{}】", bytes.length, result.length);
            }

            return result;

        } catch (IOException e) {
            log.error("对字节数组进行解压缩时方式异常，", e);
            throw new CompressException(e);
        }

    }
}
