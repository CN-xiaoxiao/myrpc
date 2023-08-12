package com.xiaoxiao.protection;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {
    // 熔断器状态
    private volatile boolean isOpen = false;
    // 总的请求数
    private AtomicInteger requestCount = new AtomicInteger(0);
    // 异常的请求数
    private AtomicInteger errorRequest = new AtomicInteger(0);
    // 最大异常数
    private int maxErrorRequest;
    private float maxErrorRate;

    public CircuitBreaker(int maxErrorRequest, float maxErrorRate) {
        this.maxErrorRequest = maxErrorRequest;
        this.maxErrorRate = maxErrorRate;
    }

    public boolean isBreak() {
        if (isOpen) {
            return true;
        }

        if (errorRequest.get() > maxErrorRequest) {
            this.isOpen = true;
            return true;
        }

        if (errorRequest.get() > 0 && requestCount.get() > 0 &&  errorRequest.get() / (float)requestCount.get() > maxErrorRate) {
            this.isOpen = true;
            return true;
        }

        return false;
    }

    public void reset() {
        this.isOpen = false;
        this.requestCount.set(0);
        this.errorRequest.set(0);
    }


    public void recordRequest() {
        this.requestCount.getAndIncrement();
    }

    public void recordErrorRequest() {
        this.errorRequest.getAndIncrement();
    }

}
