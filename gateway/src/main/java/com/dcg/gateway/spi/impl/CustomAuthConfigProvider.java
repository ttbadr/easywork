package com.dcg.gateway.spi.impl;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.dcg.gateway.model.config.auth.CustomAuthConfig;
import com.dcg.gateway.spi.AuthConfigProvider;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthConfigProvider implements AuthConfigProvider {
    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public Class<? extends AuthConfig> getAuthConfigClass() {
        return CustomAuthConfig.class;
    }
} 