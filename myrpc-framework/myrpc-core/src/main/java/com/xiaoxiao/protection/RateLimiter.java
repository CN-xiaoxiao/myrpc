package com.xiaoxiao.protection;

public interface RateLimiter {
    /**
     * 是否运行新的请求进入
     */
    boolean allowRequest();
}
