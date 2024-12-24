package com.dcg.gateway.manager;

import org.springframework.stereotype.Component;

import com.dcg.gateway.model.config.auth.TokenCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenManager {
    private final Map<String, TokenCache> tokenCaches = new ConcurrentHashMap<>();
    
    public TokenCache getTokenCache(String scheme) {
        return tokenCaches.computeIfAbsent(scheme, k -> new TokenCache());
    }
} 