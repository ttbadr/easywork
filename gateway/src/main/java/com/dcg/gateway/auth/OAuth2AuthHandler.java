package com.dcg.gateway.auth;

import com.dcg.gateway.exception.GatewayException;
import com.dcg.gateway.manager.TokenManager;
import com.dcg.gateway.model.config.auth.OAuth2AuthConfig;
import com.dcg.gateway.model.config.auth.TokenCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthHandler implements AuthHandler<OAuth2AuthConfig> {
    private final TokenManager tokenManager;
    private final WebClient webClient;

    @Override
    public String getType() {
        return "oauth2";
    }

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request,
                                              OAuth2AuthConfig config,
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
                                                         OAuth2AuthConfig config) {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCache.getToken());
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(request::header);
        }
        return request;
    }

    private void refresh(TokenCache tokenCache, OAuth2AuthConfig config) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", config.getGrantType());
            formData.add("client_id", config.getClientId());
            formData.add("client_secret", config.getClientSecret());
            if (config.getScope() != null) {
                formData.add("scope", config.getScope());
            }

            Map response = webClient.post()
                    .uri(config.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String accessToken = (String) response.get("access_token");
            // OAuth2通常会返回expires_in（秒）
            Integer expiresIn = (Integer) response.get("expires_in");
            long expiryTime = System.currentTimeMillis() + (expiresIn * 1000);
            
            tokenCache.update(accessToken, expiryTime);
        } catch (Exception e) {
            log.error("Failed to refresh OAuth2 token", e);
            throw new GatewayException("500", "Failed to refresh OAuth2 token");
        }
    }
} 