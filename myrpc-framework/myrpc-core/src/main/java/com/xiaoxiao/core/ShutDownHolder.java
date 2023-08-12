package com.xiaoxiao.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class ShutDownHolder {
    // 标记请求挡板
    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);
    // 用于请求的计算器
    public static LongAdder REQUEST_COUNTER = new LongAdder();
}
