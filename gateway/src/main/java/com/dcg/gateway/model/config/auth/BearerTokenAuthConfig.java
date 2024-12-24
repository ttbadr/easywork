package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import com.dcg.gateway.exception.GatewayException;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;

import com.dcg.gateway.manager.TokenManager;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class BearerTokenAuthConfig extends AuthConfig {
    private TokenRefreshConfig tokenRefresh;
    private final TokenManager tokenManager;

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, String scheme) {
        TokenCache tokenCache = tokenManager.getTokenCache(scheme);
        if (tokenCache.needRefresh()) {
            if (tokenCache.getRefreshLock().tryLock()) {
                try {
                    if (tokenCache.needRefresh()) {
                        refresh(tokenCache);
                    }
                } finally {
                    tokenCache.getRefreshLock().unlock();
                }
            }
        }
        return addAuthHeaders(request, tokenCache);
    }

    private WebClient.RequestHeadersSpec<?> addAuthHeaders(WebClient.RequestHeadersSpec<?> request, TokenCache tokenCache) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCache.getToken());
        if (getHeaders() != null) {
            getHeaders().forEach(request::header);
        }
        return request;
    }

    private void refresh(TokenCache tokenCache) {
        try {
            Map response = webClient.method(HttpMethod.valueOf(tokenRefresh.getMethod()))
                    .uri(tokenRefresh.getUrl())
                    .bodyValue(tokenRefresh.getBody())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String newToken = JsonPath.read(response, tokenRefresh.getTokenPath());
            long newExpireTime = System.currentTimeMillis() + 
                    Long.parseLong(tokenRefresh.getRefreshBeforeExpiry()) * 1000;
            tokenCache.update(newToken, newExpireTime);
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new GatewayException("500", "Failed to refresh token");
        }
    }
} 