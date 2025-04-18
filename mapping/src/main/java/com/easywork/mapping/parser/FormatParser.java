package com.easywork.mapping.parser;

import com.easywork.mapping.config.MapperConfig;
import java.util.Map;

/**
 * Interface for parsing input data from a specific format into the intermediate data model.
 */
public interface FormatParser {

    /**
     * Parses the input string into a map representing the intermediate data model.
     *
     * @param input The input string in a specific format (e.g., XML, JSON).
     * @param config The mapper configuration, potentially containing format-specific parsing details.
     * @return A Map<String, Object> representing the parsed data.
     * @throws Exception If parsing fails.
     */
    Map<String, Object> parse(String input, MapperConfig config) throws Exception;
}