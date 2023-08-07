package com.xiaoxiao.channelHandler.handler;

import com.xiaoxiao.MyrpcBootstrap;
import com.xiaoxiao.ServiceConfig;
import com.xiaoxiao.transport.message.MyrpcRequest;
import com.xiaoxiao.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<MyrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MyrpcRequest myrpcRequest) throws Exception {
        // 1、获取负载内容
        RequestPayload requestPayload = myrpcRequest.getRequestPayload();

        // 2、根据负载内容进行方法调用
        Object object = callTargetMethod(requestPayload);

        // Todo 3、封装响应

        // 4、写出相应
        channelHandlerContext.channel().writeAndFlush(null);
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String methodName = requestPayload.getMethodName();
        String interfaceName = requestPayload.getInterfaceName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        // 寻找匹配的暴露出去的具体实现
        ServiceConfig<?> serviceConfig = MyrpcBootstrap.SERVICE_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        Object returnValue = null;
        // 通过反射调用
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parametersValue);

        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时发生异常。",interfaceName, methodName, e);
            throw new RuntimeException(e);
        }

        return returnValue;
    }
}
