package com.dcg.gateway.model.config.auth;

import lombok.Data;
import java.util.Map;

@Data
public class TokenRefreshConfig {
    private String type;
    private String url;
    private String method;
    private Map<String, Object> body;
    private String tokenPath;
    private String refreshBeforeExpiry;
} 