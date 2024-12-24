package com.dcg.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.dcg.gateway.exception.RetryableException;
import com.dcg.gateway.model.config.DcgConfig;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final DcgConfig dcgConfig;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    if (shouldRetry(clientResponse.statusCode().value())) {
                        return Mono.error(new RetryableException());
                    }
                    return Mono.just(clientResponse);
                }))
                .filter(ExchangeFilterFunction.ofRequestProcessor(request -> 
                    Mono.just(request).retryWhen(Retry.backoff(
                        dcgConfig.getHttp().getRetry().getMaxAttempts(),
                        Duration.ofMillis(dcgConfig.getHttp().getRetry().getInitialBackoff()))
                        .maxBackoff(Duration.ofMillis(dcgConfig.getHttp().getRetry().getMaxBackoff()))
                        .filter(throwable -> throwable instanceof RetryableException)
                    )
                ));
    }

    private boolean shouldRetry(int statusCode) {
        return dcgConfig.getHttp().getRetry().getRetryOnStatus().contains(statusCode);
    }
} 