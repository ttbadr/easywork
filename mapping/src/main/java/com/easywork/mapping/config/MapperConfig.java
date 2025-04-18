package com.easywork.mapping.config;

import java.util.List;
import java.util.Map;

/**
 * Represents the overall configuration for a mapping process.
 * This class structure mirrors the YAML configuration format described in the README.
 */
public class MapperConfig {

    private String preProcessor; // Fully qualified class name of the PreProcessor implementation
    private String postProcessor; // Fully qualified class name of the PostProcessor implementation

    private FormatConfig source;
    private FormatConfig target;
    private List<MappingRule> rules;

    // Getters and Setters

    public String getPreProcessor() {
        return preProcessor;
    }

    public void setPreProcessor(String preProcessor) {
        this.preProcessor = preProcessor;
    }

    public String getPostProcessor() {
        return postProcessor;
    }

    public void setPostProcessor(String postProcessor) {
        this.postProcessor = postProcessor;
    }

    public FormatConfig getSource() {
        return source;
    }

    public void setSource(FormatConfig source) {
        this.source = source;
    }

    public FormatConfig getTarget() {
        return target;
    }

    public void setTarget(FormatConfig target) {
        this.target = target;
    }

    public List<MappingRule> getRules() {
        return rules;
    }

    public void setRules(List<MappingRule> rules) {
        this.rules = rules;
    }

    /**
     * Represents the configuration for either the source or target format.
     */
    public static class FormatConfig {
        private String format; // e.g., "json", "xml", "fixedLength"
        private ValidatorConfig validator;

        // Getters and Setters
        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public ValidatorConfig getValidator() {
            return validator;
        }

        public void setValidator(ValidatorConfig validator) {
            this.validator = validator;
        }
    }

    /**
     * Represents the configuration for a validator.
     */
    public static class ValidatorConfig {
        private String type; // e.g., "jsonSchema", "xsd", "rules", "spec"
        private Map<String, Object> config; // Validator-specific configuration parameters

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    /**
     * Represents a single mapping rule.
     */
    public static class MappingRule {
        private String source; // Source field path (e.g., JSONPath)
        private String target; // Target field path (e.g., XPath)
        private TransformConfig transform; // Optional transformation rule
        private String value; // Optional default value

        // Getters and Setters
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public TransformConfig getTransform() {
            return transform;
        }

        public void setTransform(TransformConfig transform) {
            this.transform = transform;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Represents the configuration for a value transformation.
     */
    public static class TransformConfig {
        private String type; // Type of transformation (e.g., "multiply", "dateFormat", custom class name)
        // Additional parameters specific to the transform type (e.g., factor, sourceFormat, targetFormat)
        private Map<String, Object> parameters; 

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}