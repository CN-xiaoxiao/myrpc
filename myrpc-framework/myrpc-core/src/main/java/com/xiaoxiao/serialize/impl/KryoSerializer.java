package com.xiaoxiao.serialize.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.xiaoxiao.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.*;

@Slf4j
public class KryoSerializer implements Serializer {
    private static final ThreadLocal<Kryo> kryoLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);//检测循环依赖，默认值为true,避免版本变化显式设置
        kryo.setRegistrationRequired(false);//默认值为true，避免版本变化显式设置
        ((DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
                .setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());//设定默认的实例化器
        return kryo;
    });

    @Override
    public byte[] serialize(Object object) {

        if (object == null) {
            return null;
        }

        Kryo kryo = getKryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        kryo.writeClassAndObject(output, object);
        output.close();

        if (log.isDebugEnabled()) {
            log.debug("对象【{}】已经完成了序列化操作", object);
        }

        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {

        if (bytes == null || clazz == null) {
            return null;
        }

        Kryo kryo = getKryo();
        Input input = new Input(new ByteArrayInputStream(bytes));

        if (log.isDebugEnabled()) {
            log.debug("类【{}】已经完成了反序列化操作", clazz);
        }
        return (T) kryo.readClassAndObject(input);

    }

    private Kryo getKryo() {
        return kryoLocal.get();
    }
}
