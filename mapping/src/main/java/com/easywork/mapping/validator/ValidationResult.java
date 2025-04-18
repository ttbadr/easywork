package com.easywork.mapping.validator;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the result of a format validation.
 */
public class ValidationResult {

    private boolean valid;
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * Private constructor to enforce usage of factory methods.
     */
    private ValidationResult(boolean valid) {
        this.valid = valid;
    }

    /**
     * Creates a successful validation result.
     *
     * @return A successful ValidationResult.
     */
    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    /**
     * Creates a failed validation result with a list of errors.
     *
     * @param errors The list of validation errors.
     * @return A failed ValidationResult.
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        ValidationResult result = new ValidationResult(false);
        if (errors != null) {
            result.errors.addAll(errors);
        }
        return result;
    }

    /**
     * Creates a failed validation result with a single error.
     *
     * @param error The validation error.
     * @return A failed ValidationResult.
     */
    public static ValidationResult failure(ValidationError error) {
        ValidationResult result = new ValidationResult(false);
        if (error != null) {
            result.errors.add(error);
        }
        return result;
    }

    /**
     * Checks if the validation was successful.
     *
     * @return true if valid, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the list of validation errors.
     *
     * @return The list of errors. Returns an empty list if validation was successful.
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Adds a validation error to the result.
     * This automatically marks the result as invalid.
     *
     * @param error The validation error to add.
     */
    public void addError(ValidationError error) {
        if (error != null) {
            this.valid = false;
            this.errors.add(error);
        }
    }
}