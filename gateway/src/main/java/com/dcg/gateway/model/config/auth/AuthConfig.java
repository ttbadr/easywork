package com.dcg.gateway.model.config.auth;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class AuthConfig {
    private String type;
    private Map<String, String> headers;
    protected WebClient webClient;

    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // 添加 scheme 参数
    public abstract WebClient.RequestHeadersSpec<?> auth(WebClient.RequestHeadersSpec<?> request, String scheme);
} 