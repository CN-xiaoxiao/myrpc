package com.xiaoxiao.config;

import com.xiaoxiao.IdGenerator;
import com.xiaoxiao.ProtocolConfig;
import com.xiaoxiao.compress.Compressor;
import com.xiaoxiao.compress.CompressorFactory;
import com.xiaoxiao.discovery.RegistryConfig;
import com.xiaoxiao.loadbalance.LoadBalancer;
import com.xiaoxiao.serialize.Serializer;
import com.xiaoxiao.serialize.SerializerFactory;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

@Slf4j
public class XmlResolver {

    /**
     * 从配置文件中读取配置信息
     * @param configuration 配置实例
     */
    public void loadFromXml(Configuration configuration) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("myrpc.xml");
            Document document = builder.parse(inputStream);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 解析所有的标签
            configuration.setPort(resolvePort(document, xPath));

            configuration.setAppName(resolveAppName(document, xPath));

            configuration.setIdGenerator(resolveIdGenerator(document, xPath));

            configuration.setRegistryConfig(resolveRegistryConfig(document, xPath));

            configuration.setCompressType(resolveCompressType(document, xPath));
            ObjectWrapper<Compressor> compressorObjectWrapper = resolveCompressor(document, xPath);
            CompressorFactory.addCompressor(compressorObjectWrapper);;

            configuration.setSerializeType(resolveSerializeType(document, xPath));
            ObjectWrapper<Serializer> serializerObjectWrapper = resolveSerializer(document, xPath);
            SerializerFactory.addSerializer(serializerObjectWrapper);

            configuration.setLoadBalancer(resolveLoadBalancer(document, xPath));

            configuration.setPackageName(resolvePackageName(document, xPath));

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.info("未发现配置文件或配置文件时发生了异常, 将使用默认配置", e);
        }

    }

    /**
     * 包扫描路径
     * @param document 文档
     * @param xPath xpath解析器h
     * @return 包扫描路径
     */
    private String resolvePackageName(Document document, XPath xPath) {
        String expression = "/configuration/packageName";
        return parseString(document, xPath, expression);
    }


    /**
     * 解析端口号
     * @param document 文档
     * @param xPath xpath解析器
     * @return 端口号
     */
    private int resolvePort(Document document, XPath xPath) {
        String expression = "/configuration/port";
        String s = parseString(document, xPath, expression);
        return Integer.parseInt(s);
    }

    /**
     * 解析应用名称
     * @param document 文档
     * @param xPath xpath解析器
     * @return 应用名称
     */
    private String resolveAppName(Document document, XPath xPath) {
        String expression = "/configuration/appName";
        return parseString(document, xPath, expression);
    }

    /**
     * 解析id生成器
     * @param document 文档
     * @param xPath xpath解析器
     * @return id生成器实例
     */
    private IdGenerator resolveIdGenerator(Document document, XPath xPath) {
        String expression = "/configuration/idGenerator";
        String aClass = parseString(document, xPath, expression, "class");
        String dataCenterId = parseString(document, xPath, expression, "dataCenterId");
        String machineId = parseString(document, xPath, expression, "machineId");

        Object instance = null;
        try {
            Class<?> clazz = Class.forName(aClass);
            instance = clazz.getConstructor(new Class[]{long.class, long.class})
                    .newInstance(Long.parseLong(dataCenterId), Long.parseLong(machineId));
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return (IdGenerator) instance;
    }

    /**
     * 解析注册中心
     * @param document 文档
     * @param xPath xpath解析器
     * @return 注册中心实例
     */
    private RegistryConfig resolveRegistryConfig(Document document, XPath xPath) {
        String expression = "/configuration/registry";
        String url = parseString(document, xPath, expression, "url");

        return new RegistryConfig(url);
    }

    /**
     * 解析压缩类型
     * @param document 文档
     * @param xPath xpath解析器
     * @return 压缩类型
     */
    private String resolveCompressType(Document document, XPath xPath) {
        String expression = "/configuration/compressType";
        String s = parseString(document, xPath, expression, "type");
        return s;
    }

    /**
     * 解析压缩的具体实现
     * @param document 文档
     * @param xPath xpath解析器
     * @return 压缩的具体实例
     */
    private ObjectWrapper<Compressor> resolveCompressor(Document document, XPath xPath) {
        String expression = "/configuration/compressor";
        Compressor compressor = parseObject(document, xPath, expression, null);

        byte code = Byte.parseByte(Objects.requireNonNull(parseString(document, xPath, expression, "code")));
        String name = parseString(document, xPath, expression, "name");

        return new ObjectWrapper<>(code, name, compressor);
    }

    /**
     * 解析序列化类型
     * @param document 文档
     * @param xPath xpath解析器
     * @return 序列化类型
     */
    private String resolveSerializeType(Document document, XPath xPath) {
        String expression = "/configuration/serializeType";
        String s = parseString(document, xPath, expression, "type");
        return s;

    }

    /**
     * 解析序列化实例
     * @param document 文档
     * @param xPath xpath解析器
     * @return 序列化类型
     */
    private ObjectWrapper<Serializer> resolveSerializer(Document document, XPath xPath) {
        String expression = "/configuration/serializer";
        Serializer serializer = parseObject(document, xPath, expression, null);
        byte code = Byte.parseByte(Objects.requireNonNull(parseString(document, xPath, expression, "code")));
        String name = parseString(document, xPath, expression, "name");

        return new ObjectWrapper<>(code, name, serializer);
    }

    /**
     * 配置负载均衡器
     * @param document 文档
     * @param xPath xpath解析器
     * @return 负载均衡器实例
     */
    private LoadBalancer resolveLoadBalancer(Document document, XPath xPath) {
        String expression = "/configuration/loadBalancer";
        Object object = parseObject(document, xPath, expression, null);

        return (LoadBalancer) object;
    }

    /**
     * 解析一个节点，返回一个实例
     * @param document 文档对象
     * @param xPath xpath解析器
     * @param expression 表达式
     * @param paramType 参数列表
     * @param param 参数
     * @return 配置的实例
     * @param <T> 泛型
     */
    private <T> T parseObject(Document document, XPath xPath, String expression, Class<?>[] paramType, Object... param) {
        try {
            XPathExpression expr = xPath.compile(expression);
            Node targetNode = (Node) expr.evaluate(document, XPathConstants.NODE);

            if (targetNode == null) {
                return null;
            }

            String className = targetNode.getAttributes().getNamedItem("class").getNodeValue();

            Class<?> aClass = Class.forName(className);
            Object instance = null;

            if (paramType == null) {
                instance = aClass.getConstructor().newInstance();
            } else {
                instance = aClass.getConstructor(paramType).newInstance(param);
            }

            return (T) instance;
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | XPathExpressionException e) {
            log.error("解析表达式时发生异常。", e);
        }

        return null;
    }

    /**
     * 获得一个节点属性的值
     * @param document 文档对象
     * @param xPath xpath解析器
     * @param expression 表达式
     * @param attributeName 节点名称
     * @return 节点的值
     */
    private String parseString(Document document, XPath xPath, String expression, String attributeName) {
        try {
            XPathExpression expr = xPath.compile(expression);
            Node targetNode = (Node) expr.evaluate(document, XPathConstants.NODE);

            if (targetNode == null) {
                return null;
            }

            return targetNode.getAttributes().getNamedItem(attributeName).getNodeValue();

        } catch (XPathExpressionException e) {
            log.error("解析表达式时发生异常。", e);
        }

        return null;
    }

    /**
     * 获得一个节点文本的值
     * @param document 文档对象
     * @param xPath xpath解析器
     * @param expression 表达式
     * @return 节点的值
     */
    private String parseString(Document document, XPath xPath, String expression) {
        try {
            XPathExpression expr = xPath.compile(expression);
            Node targetNode = (Node) expr.evaluate(document, XPathConstants.NODE);

            if (targetNode == null) {
                return null;
            }

            return targetNode.getTextContent();

        } catch (XPathExpressionException e) {
            log.error("解析表达式时发生异常。", e);
        }

        return null;
    }

}
