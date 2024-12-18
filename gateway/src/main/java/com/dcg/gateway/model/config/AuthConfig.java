package com.dcg.gateway.model.config;

import lombok.Data;

@Data
public class AuthConfig {
    private String type;
    private String token;
    private TokenRefreshConfig tokenRefresh;
} 