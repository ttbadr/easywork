package com.dcg.gateway.manager;

import com.dcg.gateway.exception.GatewayException;
import com.dcg.gateway.model.config.DcgConfig;
import com.dcg.gateway.model.config.SchemeConfig;
import com.dcg.gateway.model.config.ServiceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigManager {
    private final DcgConfig dcgConfig;

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigManager with config: {}", dcgConfig);
    }

    public SchemeConfig getSchemeConfig(String scheme) {
        return Optional.ofNullable(dcgConfig.getVas())
                .map(vas -> vas.get("schemes"))
                .map(schemes -> schemes.getSchemes().get(scheme))
                .orElseThrow(() -> new GatewayException("404", "Scheme not found: " + scheme));
    }

    public String getEndpoint(String scheme, String service) {
        SchemeConfig schemeConfig = getSchemeConfig(scheme);
        return Optional.ofNullable(schemeConfig.getServices())
                .map(services -> services.get(service))
                .map(endpoint -> endpoint.getEndpoint())
                .orElseThrow(() -> new GatewayException("404", 
                    String.format("Service %s not found in scheme %s", service, scheme)));
    }

    public String getContentType(String scheme) {
        return getSchemeConfig(scheme).getContentType();
    }

    public String getBaseUrl(String scheme) {
        return getSchemeConfig(scheme).getBaseUrl();
    }
} 