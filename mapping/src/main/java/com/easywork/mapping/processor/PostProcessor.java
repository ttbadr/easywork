package com.easywork.mapping.processor;

import com.easywork.mapping.config.MapperConfig;

/**
 * Interface for post-processing output data after generation.
 */
public interface PostProcessor {

    /**
     * Processes the output string after it has been generated.
     *
     * @param output The generated output string.
     * @param config The mapper configuration.
     * @return The processed output string.
     */
    String process(String output, MapperConfig config);
}