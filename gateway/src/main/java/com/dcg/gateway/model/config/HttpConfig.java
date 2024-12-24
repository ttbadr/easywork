package com.dcg.gateway.model.config;

import lombok.Data;
import java.util.List;

@Data
public class HttpConfig {
    private RetryConfig retry;

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialBackoff = 1000;
        private long maxBackoff = 5000;
        private double multiplier = 2.0;
        private List<Integer> retryOnStatus;
    }
} 