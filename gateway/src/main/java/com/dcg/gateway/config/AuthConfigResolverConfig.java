package com.dcg.gateway.config;

import com.dcg.gateway.spi.AuthConfigProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AuthConfigResolverConfig {
    
    @Autowired
    private List<AuthConfigProvider> providers;
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 通过 SPI 注册所有认证类型
        for (AuthConfigProvider provider : providers) {
            mapper.registerSubtypes(
                new NamedType(provider.getAuthConfigClass(), provider.getType())
            );
        }
        return mapper;
    }
} 