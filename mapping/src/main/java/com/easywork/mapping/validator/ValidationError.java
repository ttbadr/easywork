package com.easywork.mapping.validator;

/**
 * Represents a single validation error.
 */
public class ValidationError {

    private String fieldPath; // Path or identifier of the field with the error
    private String errorCode; // A code identifying the type of error
    private String message;   // A human-readable description of the error

    /**
     * Constructor for ValidationError.
     *
     * @param fieldPath Path or identifier of the field with the error.
     * @param errorCode A code identifying the type of error.
     * @param message   A human-readable description of the error.
     */
    public ValidationError(String fieldPath, String errorCode, String message) {
        this.fieldPath = fieldPath;
        this.errorCode = errorCode;
        this.message = message;
    }

    // Getters

    public String getFieldPath() {
        return fieldPath;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
               "fieldPath='" + fieldPath + '\'' +
               ", errorCode='" + errorCode + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}