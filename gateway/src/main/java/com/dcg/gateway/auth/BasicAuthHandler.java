package com.dcg.gateway.auth;

import com.dcg.gateway.model.config.auth.BasicAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class BasicAuthHandler implements AuthHandler<BasicAuthConfig> {

    @Override
    public String getType() {
        return "basic";
    }

    @Override
    public WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request,
                                              BasicAuthConfig config,
                                              String scheme) {
        String credentials = config.getUsername() + ":" + config.getPassword();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        request.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(request::header);
        }
        
        return request;
    }
} 