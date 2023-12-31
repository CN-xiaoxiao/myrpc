package com.xiaoxiao.exceptions;

public class ResponseException extends RuntimeException{
    private byte code;
    private String msg;
    public ResponseException() {
    }

    public ResponseException(byte code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
