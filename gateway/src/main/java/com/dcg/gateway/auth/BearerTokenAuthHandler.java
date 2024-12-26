package com.dcg.gateway.auth;

import com.dcg.gateway.exception.GatewayException;
import com.dcg.gateway.manager.TokenManager;
import com.dcg.gateway.model.config.auth.BearerTokenAuthConfig;
import com.dcg.gateway.model.config.auth.TokenCache;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BearerTokenAuthHandler implements AuthHandler<BearerTokenAuthConfig> {
    private final TokenManager tokenManager;
    private final WebClient webClient;

    @Override
    public String getType() {
        return "bearer_token";
    }

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, 
                                              BearerTokenAuthConfig config,
                                              String scheme) {
        TokenCache tokenCache = tokenManager.getTokenCache(scheme);
        if (tokenCache.needRefresh()) {
            if (tokenCache.getRefreshLock().tryLock()) {
                try {
                    if (tokenCache.needRefresh()) {
                        refresh(tokenCache, config);
                    }
                } finally {
                    tokenCache.getRefreshLock().unlock();
                }
            }
        }
        return addAuthHeaders(request, tokenCache, config);
    }

    private WebClient.RequestHeadersSpec<?> addAuthHeaders(WebClient.RequestHeadersSpec<?> request, 
                                                         TokenCache tokenCache,
                                                         BearerTokenAuthConfig config) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCache.getToken());
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(request::header);
        }
        return request;
    }

    private void refresh(TokenCache tokenCache, BearerTokenAuthConfig config) {
        try {
            var tokenRefresh = config.getTokenRefresh();
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