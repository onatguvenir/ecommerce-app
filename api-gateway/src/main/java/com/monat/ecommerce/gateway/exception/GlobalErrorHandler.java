package com.monat.ecommerce.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global Error Handler for the Reactive API Gateway.
 * 
 * Educational Note:
 * Since Spring Cloud Gateway is based on Project Reactor (WebFlux), 
 * we use ErrorWebExceptionHandler instead of @ControllerAdvice.
 * This ensures that errors occurring in the hidden 'filter chain' are also 
 * caught and returned as clean JSON.
 */
@Slf4j
@Component
@Order(-1)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "Service not found";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason();
        }

        response.setStatusCode(status);

        log.error("Gateway error: {}", ex.getMessage(), ex);

        String errorJson = """
                {
                  "error": "%s",
                  "message": "%s",
                  "status": %d
                }
                """.formatted(status.getReasonPhrase(), message, status.value());

        DataBuffer buffer = response.bufferFactory()
                .wrap(errorJson.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
