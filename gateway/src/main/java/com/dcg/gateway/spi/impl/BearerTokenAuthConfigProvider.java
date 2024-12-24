package com.dcg.gateway.spi.impl;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.dcg.gateway.model.config.auth.BearerTokenAuthConfig;
import com.dcg.gateway.spi.AuthConfigProvider;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenAuthConfigProvider implements AuthConfigProvider {
    @Override
    public String getType() {
        return "bearer_token";
    }

    @Override
    public Class<? extends AuthConfig> getAuthConfigClass() {
        return BearerTokenAuthConfig.class;
    }
} 