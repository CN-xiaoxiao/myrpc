package com.xiaoxiao.serialize.impl;

import com.xiaoxiao.exceptions.SerializerException;
import com.xiaoxiao.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {

        if (object == null) {
            return null;
        }

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos)) {

            outputStream.writeObject(object);

            if (log.isDebugEnabled()) {
                log.debug("对象【{}】已经完成了序列化操作", object);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            log.error("序列化对象【{}】时发生异常。",object);
            throw new SerializerException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {

        if (bytes == null || clazz == null) {
            return null;
        }

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais)) {

            Object object = ois.readObject();

            if (log.isDebugEnabled()) {
                log.debug("类【{}】已经完成了反序列化操作", clazz);
            }

            return (T)object;

        } catch (IOException | ClassNotFoundException e) {
            log.error("反序列化对象【{}】时发生异常",clazz);
            throw new SerializerException(e);
        }

    }
}
