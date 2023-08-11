package com.xiaoxiao.exceptions;

public class SpiException extends RuntimeException{
    public SpiException() {
    }

    public SpiException(String message) {
        super(message);
    }

    public SpiException(Throwable cause) {
        super(cause);
    }

    public SpiException(String message, Throwable cause) {
        super(message, cause);
    }
}
