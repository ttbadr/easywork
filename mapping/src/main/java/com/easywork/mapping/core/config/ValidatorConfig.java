package com.easywork.mapping.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证器配置类
 * 用于配置验证器的行为和规则
 */
public class ValidatorConfig {
    private String schemaPath;
    private Map<String, Object> parameters;

    public ValidatorConfig() {
        this.parameters = new HashMap<>();
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
}