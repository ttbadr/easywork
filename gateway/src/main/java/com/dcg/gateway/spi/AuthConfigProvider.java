package com.dcg.gateway.spi;

import com.dcg.gateway.model.config.auth.AuthConfig;

public interface AuthConfigProvider {
    String getType();
    Class<? extends AuthConfig> getAuthConfigClass();
} 