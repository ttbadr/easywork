package com.dcg.gateway.filter;

import com.dcg.gateway.auth.AuthHandlerManager;
import com.dcg.gateway.exception.GatewayException;
import com.dcg.gateway.manager.ConfigManager;
import com.dcg.gateway.model.GatewayRequest;
import com.dcg.gateway.model.GatewayResponse;
import com.dcg.gateway.model.config.SchemeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DcgGatewayFilter extends AbstractGatewayFilterFactory<DcgGatewayFilter.Config> {
    private final ObjectMapper objectMapper;
    private final ConfigManager configManager;
    private final WebClient webClient;
    private final AuthHandlerManager authHandlerManager;

    public DcgGatewayFilter(ObjectMapper objectMapper, 
                           ConfigManager configManager,
                           WebClient.Builder webClientBuilder,
                           AuthHandlerManager authHandlerManager) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.configManager = configManager;
        this.webClient = webClientBuilder.build();
        this.authHandlerManager = authHandlerManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        
                        try {
                            String body = new String(bytes, StandardCharsets.UTF_8);
                            GatewayRequest<?> request = objectMapper.readValue(body, GatewayRequest.class);
                            return handleRequest(exchange, request);
                        } catch (Exception e) {
                            log.error("Failed to parse request", e);
                            return Mono.error(new GatewayException("400", "Invalid request format"));
                        }
                    })
                    .onErrorResume(error -> handleError(exchange, error));
        };
    }

    private Mono<Void> handleRequest(ServerWebExchange exchange, GatewayRequest<?> request) {
        try {
            SchemeConfig schemeConfig = configManager.getSchemeConfig(request.getScheme());
            String endpoint = configManager.getEndpoint(request.getScheme(), request.getService());

            Object requestBody = request.getData();
            log.info("Forwarding request to {}, body: {}", endpoint, requestBody);

            WebClient.RequestHeadersSpec<?> requestSpec = webClient.method(HttpMethod.POST)
                    .uri(endpoint)
                    .contentType(getMediaType(schemeConfig.getContentType()))
                    .bodyValue(requestBody);

            // 使用AuthHandlerManager处理认证
            if (schemeConfig.getAuthConfig() != null) {
                requestSpec = authHandlerManager.auth(requestSpec, schemeConfig.getAuthConfig(), request.getScheme());
            }

            return requestSpec.exchangeToMono(clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(responseBody -> {
                            try {
                                Object responseData;
                                if ("json".equalsIgnoreCase(schemeConfig.getContentType())) {
                                    responseData = objectMapper.readValue(responseBody, Object.class);
                                } else {
                                    responseData = responseBody;
                                }

                                GatewayResponse<?> response = GatewayResponse.success(responseData);
                                byte[] bytes = objectMapper.writeValueAsBytes(response);
                                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                                
                                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                return exchange.getResponse().writeWith(Mono.just(buffer));
                            } catch (Exception e) {
                                return Mono.error(new GatewayException("500", "Failed to process response"));
                            }
                        });
            });
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        log.error("Error processing request", error);
        GatewayResponse<?> response;
        if (error instanceof GatewayException) {
            GatewayException ge = (GatewayException) error;
            response = GatewayResponse.error(ge.getCode(), ge.getMessage());
        } else {
            response = GatewayResponse.error("500", "Internal Server Error");
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private MediaType getMediaType(String contentType) {
        return "xml".equalsIgnoreCase(contentType) ? 
                MediaType.APPLICATION_XML : 
                MediaType.APPLICATION_JSON;
    }

    public static class Config {
        // Configuration properties if needed
    }
} 