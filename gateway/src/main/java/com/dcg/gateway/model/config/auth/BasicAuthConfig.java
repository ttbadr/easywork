package com.dcg.gateway.model.config.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasicAuthConfig extends AuthConfig {
    private String username;
    private String password;
} 