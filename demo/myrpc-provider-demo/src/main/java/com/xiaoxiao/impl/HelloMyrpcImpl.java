package com.xiaoxiao.impl;

import com.xiaoxiao.HelloMyrpc;
import com.xiaoxiao.annotation.MyrpcApi;

@MyrpcApi(group = "primary")
public class HelloMyrpcImpl implements HelloMyrpc {

    @Override
    public String sayHi(String msg) {
        return "hi consumer: " + msg;
    }
}
