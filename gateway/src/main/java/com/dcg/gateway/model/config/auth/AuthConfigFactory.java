package com.dcg.gateway.model.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class AuthConfigFactory {
    private final ObjectMapper objectMapper;

    public AuthConfigFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthConfig create(Map<String, Object> config) {
        if (config == null || !config.containsKey("type")) {
            return null;
        }
        return objectMapper.convertValue(config, AuthConfig.class);
    }
} 