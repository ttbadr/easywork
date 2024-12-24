package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomAuthConfig extends AuthConfig {
    private String apiKey;
    private String apiSecret;

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, String scheme) {
        if (getHeaders() != null) {
            getHeaders().forEach(request::header);
        }
        return request;
    }
} 