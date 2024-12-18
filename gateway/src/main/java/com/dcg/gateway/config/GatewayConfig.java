package com.dcg.gateway.config;

import com.dcg.gateway.filter.DcgGatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, DcgGatewayFilter filter) {
        return builder.routes()
                .route("dcg_route", r -> r.path("/**")
                        .filters(f -> f.filter(filter.apply(new DcgGatewayFilter.Config())))
                        .uri("no://op"))
                .build();
    }
} 