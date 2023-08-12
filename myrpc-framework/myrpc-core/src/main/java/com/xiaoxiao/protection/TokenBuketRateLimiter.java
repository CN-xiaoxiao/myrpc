package com.xiaoxiao.protection;

/**
 * 基于令牌桶的限流器
 */
public class TokenBuketRateLimiter implements RateLimiter{

    // 令牌的数量， >0 有令牌，放行， ==0无令牌
    private int tokens;
    private int capacity;
    private int rate;

    private Long lastTokenTime;

    public TokenBuketRateLimiter(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        lastTokenTime = System.currentTimeMillis();
        tokens = capacity;
    }

    /**
     * 
     * @return
     */
    public synchronized boolean allowRequest() {

        long currentTime = System.currentTimeMillis();
        long timeInterval =  currentTime - lastTokenTime;

        if (timeInterval >= 1000/rate) {
            int needAddTokens = (int) (timeInterval * rate / 1000);
            tokens = Math.min(capacity, tokens + needAddTokens);
            this.lastTokenTime = System.currentTimeMillis();
        }

        if (tokens > 0) {
            tokens--;
            return true;
        } else{
            return false;
        }
    }
}
