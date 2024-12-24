package com.dcg.gateway.model;

import lombok.Data;

@Data
public class GatewayRequest<T> {
    private String scheme;
    private String service;
    private T data;
} 