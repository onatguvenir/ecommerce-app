package com.monat.ecommerce.common.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for all REST endpoints.
 * Provides a consistent structure for success and basic error scenarios.
 * Uses Java 17+ Records for immutability and concise syntax.
 */
@Builder
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp,
        String traceId // Essential for distributed tracing correlation
) {
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
