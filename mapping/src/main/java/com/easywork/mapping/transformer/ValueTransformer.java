package com.easywork.mapping.transformer;

import java.util.Map;

/**
 * Interface for implementing custom value transformation logic.
 */
public interface ValueTransformer {

    /**
     * Transforms the input value based on the provided parameters.
     *
     * @param value The input value to transform.
     * @param parameters A map containing configuration parameters for the transformer (e.g., format strings, factors).
     * @return The transformed value.
     * @throws Exception If transformation fails.
     */
    Object transform(Object value, Map<String, Object> parameters) throws Exception;
}