package com.dcg.gateway.config;

import com.dcg.gateway.model.config.DcgConfig;
import com.dcg.gateway.model.config.SchemeConfig;
import com.dcg.gateway.model.config.auth.AuthConfigFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SchemeConfigProcessor implements BeanPostProcessor {
    private final AuthConfigFactory authConfigFactory;

    public SchemeConfigProcessor(AuthConfigFactory authConfigFactory) {
        this.authConfigFactory = authConfigFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DcgConfig) {
            DcgConfig config = (DcgConfig) bean;
            if (config.getVas() != null && config.getVas().getSchemes() != null) {
                for (SchemeConfig scheme : config.getVas().getSchemes().values()) {
                    processSchemeConfig(scheme);
                }
            }
        }
        return bean;
    }

    private void processSchemeConfig(SchemeConfig scheme) {
        if (scheme.getAuth() != null) {
            scheme.setAuthConfig(authConfigFactory.create(scheme.getAuth()));
        }
    }
} 