package com.easywork.mapping.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析器配置类
 * 用于配置格式解析器的行为
 */
public class ParserConfig {
    private Map<String, Object> parameters;
    private String encoding;
    private boolean strictMode;

    public ParserConfig() {
        this.parameters = new HashMap<>();
        this.encoding = "UTF-8";
        this.strictMode = true;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
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