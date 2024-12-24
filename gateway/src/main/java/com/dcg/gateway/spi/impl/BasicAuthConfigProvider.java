package com.dcg.gateway.spi.impl;

import com.dcg.gateway.model.config.auth.AuthConfig;
import com.dcg.gateway.model.config.auth.BasicAuthConfig;
import com.dcg.gateway.spi.AuthConfigProvider;
import org.springframework.stereotype.Component;

@Component
public class BasicAuthConfigProvider implements AuthConfigProvider {
    @Override
    public String getType() {
        return "basic";
    }

    @Override
    public Class<? extends AuthConfig> getAuthConfigClass() {
        return BasicAuthConfig.class;
    }
} 