package com.xiaoxiao.impl;

import com.xiaoxiao.HelloMyrpc;

public class HelloMyrpcImpl implements HelloMyrpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer: " + msg;
    }
}
