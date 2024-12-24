package com.dcg.gateway.model;

import lombok.Data;

@Data
public class GatewayResponse<T> {
    private String code;
    private String message;
    private T data;

    public static <T> GatewayResponse<T> success(T data) {
        GatewayResponse<T> response = new GatewayResponse<>();
        response.setCode("200");
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> GatewayResponse<T> error(String code, String message) {
        GatewayResponse<T> response = new GatewayResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
} 