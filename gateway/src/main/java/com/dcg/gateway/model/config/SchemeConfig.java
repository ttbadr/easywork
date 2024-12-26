package com.dcg.gateway.model.config;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.Map;

@Data
public class SchemeConfig {
    private String baseUrl;
    private String contentType;
    private Map<String, EndpointConfig> services;
    private Map<String, Object> auth;
    
    @JsonIgnore
    private AuthConfig authConfig;
    
    public AuthConfig getAuthConfig() {
        return authConfig;
    }
    
    public void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }
    
    public Map<String, Object> getAuth() {
        return auth;
    }
    
    public void setAuth(Map<String, Object> auth) {
        this.auth = auth;
    }
} 