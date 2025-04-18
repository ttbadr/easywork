package com.easywork.mapping.generator;

import com.easywork.mapping.config.MapperConfig;
import java.util.Map;

/**
 * Interface for generating output data in a specific format from the intermediate data model.
 */
public interface FormatGenerator {

    /**
     * Generates an output string in a specific format from the intermediate data model.
     *
     * @param data The intermediate data model (Map<String, Object>).
     * @param config The mapper configuration, potentially containing format-specific generation details.
     * @return The generated output string.
     * @throws Exception If generation fails.
     */
    String generate(Map<String, Object> data, MapperConfig config) throws Exception;
}