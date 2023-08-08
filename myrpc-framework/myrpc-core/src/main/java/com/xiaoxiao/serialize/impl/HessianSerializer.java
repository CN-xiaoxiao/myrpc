package com.xiaoxiao.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.xiaoxiao.exceptions.SerializerException;
import com.xiaoxiao.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {

        if (object == null) {
            return null;
        }


        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Hessian2Output hessian2Output = new Hessian2Output(baos);
            hessian2Output.writeObject(object);
            hessian2Output.flush();

            if (log.isDebugEnabled()) {
                log.debug("对象使用hessian【{}】完成了序列化操作", object);
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
            ) {

            Hessian2Input hessian2Input = new Hessian2Input(bais);
            T object = (T)hessian2Input.readObject();

            if (log.isDebugEnabled()) {
                log.debug("类使用hessian【{}】已经完成了反序列化操作", clazz);
            }

            return object;

        } catch (IOException e) {
            log.error("反序列化对象【{}】时发生异常",clazz);
            throw new SerializerException(e);
        }

    }
}
