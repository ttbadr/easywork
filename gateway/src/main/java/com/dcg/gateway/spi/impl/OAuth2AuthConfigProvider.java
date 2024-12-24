package com.dcg.gateway.spi.impl;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.dcg.gateway.model.config.auth.OAuth2AuthConfig;
import com.dcg.gateway.spi.AuthConfigProvider;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthConfigProvider implements AuthConfigProvider {
    @Override
    public String getType() {
        return "oauth2";
    }

    @Override
    public Class<? extends AuthConfig> getAuthConfigClass() {
        return OAuth2AuthConfig.class;
    }
} 