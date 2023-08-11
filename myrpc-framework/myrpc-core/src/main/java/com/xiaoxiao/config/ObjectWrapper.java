package com.xiaoxiao.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectWrapper<T> {
    private byte code;
    private String type;
    private T impl;
}
