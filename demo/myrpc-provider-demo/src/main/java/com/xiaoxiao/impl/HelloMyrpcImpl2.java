package com.xiaoxiao.impl;

import com.xiaoxiao.HelloMyrpc;
import com.xiaoxiao.HelloMyrpc2;
import com.xiaoxiao.annotation.MyrpcApi;

@MyrpcApi
public class HelloMyrpcImpl2 implements HelloMyrpc2 {

    @Override
    public String sayHi(String msg) {
        return "hi consumer: " + msg;
    }
}
