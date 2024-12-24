package com.dcg.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConfigChangeListener {
    private static final Logger log = LoggerFactory.getLogger(ConfigChangeListener.class);

    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefresh(RefreshScopeRefreshedEvent event) {
        // 处理配置刷新后的逻辑
        log.info("Configuration refreshed");
    }
} 