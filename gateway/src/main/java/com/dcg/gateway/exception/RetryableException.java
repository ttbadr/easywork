package com.dcg.gateway.exception;

public class RetryableException extends RuntimeException {
    public RetryableException() {
        super("Request failed with retryable status code");
    }
} 