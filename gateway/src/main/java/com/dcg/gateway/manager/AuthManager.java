package com.dcg.gateway.manager;

import com.dcg.gateway.model.config.AuthConfig;
import com.dcg.gateway.model.config.SchemeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthManager {
    private final ConfigManager configManager;
    private final RestTemplate restTemplate;
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    public String getToken(String scheme) {
        return tokenCache.computeIfAbsent(scheme, this::refreshToken);
    }

    private String refreshToken(String scheme) {
        SchemeConfig schemeConfig = configManager.getSchemeConfig(scheme);
        AuthConfig authConfig = schemeConfig.getAuth();
        // TODO: Implement token refresh logic
        return "";
    }

    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkTokens() {
        // TODO: Implement token check and refresh logic
    }
} 