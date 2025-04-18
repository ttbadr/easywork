package com.easywork.mapping.processor;

import com.easywork.mapping.config.MapperConfig;

/**
 * Interface for pre-processing input data before parsing.
 */
public interface PreProcessor {

    /**
     * Processes the input string before it is parsed.
     *
     * @param input The raw input string.
     * @param config The mapper configuration.
     * @return The processed input string.
     */
    String process(String input, MapperConfig config);
}