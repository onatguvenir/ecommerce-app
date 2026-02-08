package com.monat.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global logging filter for all requests
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Generate correlation ID for request tracking
        String correlationId = UUID.randomUUID().toString();
        
        log.info("Incoming request: {} {} - Correlation ID: {}", 
                request.getMethod(), 
                request.getURI().getPath(),
                correlationId);

        // Add correlation ID to request headers
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-Correlation-Id", correlationId)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    log.info("Response: {} - Status: {} - Correlation ID: {}",
                            request.getURI().getPath(),
                            response.getStatusCode(),
                            correlationId);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
