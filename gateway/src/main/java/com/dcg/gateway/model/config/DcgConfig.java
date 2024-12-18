package com.dcg.gateway.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "dcg")
public class DcgConfig {
    private Map<String, ServiceConfig> vas;
} 