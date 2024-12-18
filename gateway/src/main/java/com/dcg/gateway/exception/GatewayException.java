package com.dcg.gateway.exception;

import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {
    private final String code;

    public GatewayException(String code, String message) {
        super(message);
        this.code = code;
    }

    public GatewayException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
} 