package com.xiaoxiao.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用来描述请求调用方所请求的接口方法的描述
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPayload implements Serializable {

    // 接口的名字   {com.xiaoxiao.HelloMyrpc}
    private String interfaceName;
    // 方法的名字   {sayHi}
    private String methodName;
    // 参数列表
    private Class<?>[] parametersType;
    private Object[] parametersValue;

    // 返回值类型
    private Class<?> returnType;
}
