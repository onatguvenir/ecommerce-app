package com.monat.ecommerce.common.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Detailed error response structure used by the GlobalExceptionHandler.
 * Captures status codes, correlation IDs, and validation-specific details.
 */
@Builder
public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        LocalDateTime timestamp,
        String traceId, // Correlates with Jaeger/Logback traces
        Map<String, String> validationErrors // Specifically for @Valid / MethodArgumentNotValidException
) {
}
