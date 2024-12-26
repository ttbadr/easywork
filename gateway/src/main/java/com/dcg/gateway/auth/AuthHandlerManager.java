package com.dcg.gateway.auth;

import com.dcg.gateway.model.config.auth.AuthConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AuthHandlerManager {
    private final Map<String, AuthHandler<AuthConfig>> handlers;

    @SuppressWarnings("unchecked")
    public AuthHandlerManager(List<AuthHandler<?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(
                        AuthHandler::getType,
                        h -> (AuthHandler<AuthConfig>) h
                ));
    }

    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, 
                                              AuthConfig config, 
                                              String scheme) {
        AuthHandler<AuthConfig> handler = handlers.get(config.getType());
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported auth type: " + config.getType());
        }
        return handler.auth(request, config, scheme);
    }
} 