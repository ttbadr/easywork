package com.dcg.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "dcg.config.refresh.enabled", havingValue = "true", matchIfMissing = false)
public class RefreshConfig {
    // 配置自动刷新相关的功能可以在这里添加
} 