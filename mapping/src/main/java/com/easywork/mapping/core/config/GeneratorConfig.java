package com.easywork.mapping.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 生成器配置类
 * 用于配置格式生成器的行为
 */
public class GeneratorConfig {
    private Map<String, Object> parameters;
    private String encoding;
    private boolean prettyPrint;
    private boolean validateOutput;

    public GeneratorConfig() {
        this.parameters = new HashMap<>();
        this.encoding = "UTF-8";
        this.prettyPrint = false;
        this.validateOutput = true;
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

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isValidateOutput() {
        return validateOutput;
    }

    public void setValidateOutput(boolean validateOutput) {
        this.validateOutput = validateOutput;
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