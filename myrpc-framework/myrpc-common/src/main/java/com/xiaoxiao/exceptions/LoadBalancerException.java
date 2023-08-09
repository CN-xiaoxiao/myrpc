package com.xiaoxiao.exceptions;

public class LoadBalancerException extends RuntimeException{
    public LoadBalancerException() {
    }

    public LoadBalancerException(String message) {
        super(message);
    }

    public LoadBalancerException(Throwable cause) {
        super(cause);
    }
}
