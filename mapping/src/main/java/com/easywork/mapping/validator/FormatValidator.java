package com.easywork.mapping.validator;

import com.easywork.mapping.config.MapperConfig;

/**
 * Interface for validating input or output data against a specific format.
 */
public interface FormatValidator {

    /**
     * Validates the given input string based on the provided configuration.
     *
     * @param input The input string to validate.
     * @param config The mapper configuration containing validation rules or schema references.
     * @return A ValidationResult object indicating whether the input is valid and any errors found.
     */
    ValidationResult validate(String input, MapperConfig config);
}