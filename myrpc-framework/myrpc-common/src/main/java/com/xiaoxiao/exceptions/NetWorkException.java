package com.xiaoxiao.exceptions;

public class NetWorkException extends RuntimeException{
    public NetWorkException() {
    }

    public NetWorkException(String message) {
        super(message);
    }

    public NetWorkException(Throwable cause) {
        super(cause);
    }
}
