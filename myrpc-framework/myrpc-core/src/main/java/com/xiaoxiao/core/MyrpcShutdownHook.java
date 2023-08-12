package com.xiaoxiao.core;

import java.util.concurrent.TimeUnit;

public class MyrpcShutdownHook extends Thread {
    @Override
    public void run() {
        // 1、打开“挡板”
        ShutDownHolder.BAFFLE.set(true);

        long start = System.currentTimeMillis();
        // 2、等待计算器归零（正常的请求处理结束, 等待10秒
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (ShutDownHolder.REQUEST_COUNTER.sum() == 0L
                    && System.currentTimeMillis() - start > 10000) {
                break;
            }
        }
        // 3、阻塞结束后，放行，执行其他操作
    }
}
