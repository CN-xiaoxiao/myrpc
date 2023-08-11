package com.xiaoxiao.spi;

import com.xiaoxiao.config.ObjectWrapper;
import com.xiaoxiao.exceptions.SpiException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 实现一个spi
 */
@Slf4j
public class SpiHandler {

    private static final String BASE_PATH = "META-INF/myrpc-services";

    private static final Map<String, List<String>> SPI_CONTENT = new ConcurrentHashMap<>(8);
    // 缓存每一个接口所对应的实现的实例
    private static final Map<Class<?>, List<ObjectWrapper<?>>> SPI_IMPLEMENT = new ConcurrentHashMap<>(32);

    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fileUrl = classLoader.getResource(BASE_PATH);

        if (fileUrl!= null) {
            File file = new File(fileUrl.getPath());
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    String key = child.getName();
                    List<String> value = getImplNames(child);
                    SPI_CONTENT.put(key, value);
                }
            }
        }
    }

    /**
     * 获取一个和当前服务相关的实例
     * @param clazz 服务接口的class实例
     * @return 一个实现类的实例
     */
    public synchronized static <T> ObjectWrapper<T> get(Class<T> clazz) {

        List<ObjectWrapper<?>> objectWrappers = SPI_IMPLEMENT.get(clazz);

        if (objectWrappers != null && objectWrappers.size() > 0) {
            return (ObjectWrapper<T>) objectWrappers.get(0);
        }

        buildCache(clazz);

        List<ObjectWrapper<?>> objectWrapperList = SPI_IMPLEMENT.get(clazz);
        if (objectWrapperList == null || objectWrapperList.size() == 0) {
            return null;
        }

        return (ObjectWrapper<T>) objectWrapperList.get(0);
    }

    /**
     * 获取所有和当前服务相关的实例
     * @param clazz 服务接口的class实例
     * @return 实现类的实例集合
     * @param <T> 泛型
     */
    public synchronized static <T> List<ObjectWrapper<T>> getList(Class<T> clazz) {

        List<ObjectWrapper<?>> objectWrappers = SPI_IMPLEMENT.get(clazz);

        if (objectWrappers != null && objectWrappers.size() > 0) {
            return objectWrappers.stream().map( wrapper -> (ObjectWrapper<T>) wrapper).collect(Collectors.toList());
        }

        buildCache(clazz);

        List<ObjectWrapper<?>> list = SPI_IMPLEMENT.get(clazz);
        if (list != null && list.size() > 0) {
            return list.stream().map( wrapper -> (ObjectWrapper<T>) wrapper).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * 构建clazz相关的所有类
     * @param clazz
     */
    private static void buildCache(Class<?> clazz) {

        String name = clazz.getName();
        List<String> implNames = SPI_CONTENT.get(name);

        if (implNames == null || implNames.size() == 0) {
            return;
        }

        List<ObjectWrapper<?>> instance = new ArrayList<>();
        for (String implName : implNames) {
            try {
                String[] split = implName.split("-");
                if (split.length != 3) {
                    throw new SpiException("您配置的spi文件不合法");
                }

                byte code = Byte.parseByte(split[0]);
                String type = split[1];
                String implementName = split[2];

                Class<?> aClass = Class.forName(implementName);
                Object impl = aClass.getConstructor().newInstance();

                ObjectWrapper<?> objectWrapper = new ObjectWrapper<>(code,type, impl);

                instance.add(objectWrapper);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                log.error("实例化【{}】的实现时，发生异常", implName, e);
            }
        }

        SPI_IMPLEMENT.put(clazz, instance);

    }

    /**
     * 获取文件内容的所有实现名称
     * @param child 文件对象
     * @return 实现类的全限定名称集合
     */
    private static List<String> getImplNames(File child) {

        try (FileReader fileReader = new FileReader(child);
             BufferedReader bufferedReader = new BufferedReader(fileReader)){

            List<String> implList = new ArrayList<>();

            while (true) {
                String line = bufferedReader.readLine();
                if (line == null || "".equals(line)) {
                    break;
                }
                implList.add(line);
            }

            return implList;
        } catch (IOException e) {
            log.error("读取spi文件时发生异常");
        }

        return null;
    }
}
