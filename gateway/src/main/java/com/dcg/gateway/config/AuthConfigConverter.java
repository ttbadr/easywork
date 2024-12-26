package com.dcg.gateway.config;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationPropertiesBinding
public class AuthConfigConverter implements Converter<Map<String, Object>, AuthConfig> {
    private final ObjectMapper objectMapper;

    public AuthConfigConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AuthConfig convert(Map<String, Object> source) {
        if (source == null || !source.containsKey("type")) {
            return null;
        }
        try {
            // 使用已配置的ObjectMapper（包含了所有认证类型信息）
            return objectMapper.convertValue(source, AuthConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert auth config: " + e.getMessage(), e);
        }
    }
} 