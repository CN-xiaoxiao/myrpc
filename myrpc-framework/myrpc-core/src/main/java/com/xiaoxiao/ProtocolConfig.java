package com.xiaoxiao;

import lombok.ToString;

@ToString
public class ProtocolConfig {
    private String protocolName;

    public ProtocolConfig(String protocolName) {
        this.protocolName = protocolName;
    }
}
