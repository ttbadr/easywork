package com.dcg.gateway.model.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.Map;

@Getter
@ConfigurationProperties(prefix = "dcg")
@RefreshScope
@ConstructorBinding
public class DcgConfig {
    private final Vas vas;
    private final HttpConfig http;

    public DcgConfig(Vas vas, HttpConfig http) {
        this.vas = vas;
        this.http = http;
    }

    @Getter
    public static class Vas {
        private final Map<String, SchemeConfig> schemes;

        public Vas(Map<String, SchemeConfig> schemes) {
            this.schemes = schemes;
        }
    }
} 