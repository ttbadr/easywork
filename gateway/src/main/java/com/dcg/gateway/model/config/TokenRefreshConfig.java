package com.dcg.gateway.model.config;

import lombok.Data;
import java.util.Map;

@Data
public class TokenRefreshConfig {
    private String type;
    private String url;
    private String method;
    private Map<String, String> body;
    private String tokenPath;
    private String refreshBeforeExpiry;
    private Integer maxRetryTimes;
    private String retryInterval;
} 