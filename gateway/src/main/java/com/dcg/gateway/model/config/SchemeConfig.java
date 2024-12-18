package com.dcg.gateway.model.config;

import lombok.Data;
import java.util.Map;

@Data
public class SchemeConfig {
    private String baseUrl;
    private String contentType;
    private Map<String, EndpointConfig> services;
    private AuthConfig auth;
} 