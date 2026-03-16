package com.monat.ecommerce.common.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error response structure for exception handling
 */
@Builder
public record ErrorResponse(
    String error,
    String message,
    int status,
    String path,
    LocalDateTime timestamp,
    String traceId,
    Map<String, String> validationErrors
) {}
