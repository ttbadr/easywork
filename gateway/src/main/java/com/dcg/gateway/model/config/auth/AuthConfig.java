package com.dcg.gateway.model.config.auth;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class AuthConfig {
    private String type;
    private Map<String, String> headers;
} 