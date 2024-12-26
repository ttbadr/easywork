package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomAuthConfig extends AuthConfig {
    private String apiKey;
    private String apiSecret;
} 