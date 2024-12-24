package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.dcg.gateway.manager.TokenManager;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class OAuth2AuthConfig extends AuthConfig {
    private final TokenManager tokenManager;

    private String grantType = "client_credentials";
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String authUrl;
    private String refreshToken;
    private String scope;
    private Map<String, String> additionalParams;
    private Integer tokenExpiresIn;
    private String tokenType = "Bearer";
    private String accessToken;

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, String scheme) {
        TokenCache tokenCache = tokenManager.getTokenCache(scheme);
        if (tokenCache.needRefresh()) {
            // ... token 刷新逻辑
        }
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCache.getToken());
    }
} 