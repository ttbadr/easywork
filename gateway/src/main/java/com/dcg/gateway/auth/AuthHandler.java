package com.dcg.gateway.auth;

import com.dcg.gateway.model.config.auth.AuthConfig;
import org.springframework.web.reactive.function.client.WebClient;

public interface AuthHandler<T extends AuthConfig> {
    /**
     * 获取此处理器支持的认证类型
     */
    String getType();

    /**
     * 处理认证逻辑
     */
    WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, T config, String scheme);
} 