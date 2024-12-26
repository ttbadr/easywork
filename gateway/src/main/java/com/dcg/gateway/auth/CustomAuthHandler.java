package com.dcg.gateway.auth;

import com.dcg.gateway.model.config.auth.CustomAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CustomAuthHandler implements AuthHandler<CustomAuthConfig> {

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request,
                                              CustomAuthConfig config,
                                              String scheme) {
        // 自定义认证逻辑
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(request::header);
        }
        
        return request;
    }
} 