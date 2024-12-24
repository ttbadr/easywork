package com.dcg.gateway.model.config;

import lombok.Data;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dcg")
@RefreshScope
public class DcgConfig {
    private Vas vas;
    private HttpConfig http;

    @Data
    public static class Vas {
        private Map<String, SchemeConfig> schemes;
    }
} 