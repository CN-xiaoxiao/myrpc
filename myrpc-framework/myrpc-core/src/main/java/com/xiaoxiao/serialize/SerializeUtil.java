package com.xiaoxiao.serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerializeUtil {

    public static byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            outputStream.writeObject(object);


            return baos.toByteArray();
        } catch (IOException e) {
//            log.error("序列化时发生异常。");
            throw new RuntimeException(e);
        }
    }
}
