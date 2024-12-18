package com.dcg.gateway.model.config;

import lombok.Data;
import java.util.Map;

@Data
public class ServiceConfig {
    private Map<String, SchemeConfig> schemes;
} 