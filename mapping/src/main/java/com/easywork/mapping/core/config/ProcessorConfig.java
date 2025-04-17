package com.easywork.mapping.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理器配置类
 * 用于配置预处理器和后处理器的行为
 */
public class ProcessorConfig {
    private Map<String, Object> parameters;

    public ProcessorConfig() {
        this.parameters = new HashMap<>();
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

    public Object getParameter(String key, Object defaultValue) {
        return this.parameters.getOrDefault(key, defaultValue);
    }
}