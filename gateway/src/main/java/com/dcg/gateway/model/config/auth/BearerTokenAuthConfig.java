package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BearerTokenAuthConfig extends AuthConfig {
    private String token;
    private TokenRefreshConfig tokenRefresh;
} 