package com.monat.ecommerce.user.application.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Response DTO for successful authentication.
 */
@Builder
public record AuthResponse(
    UUID userId,
    String accessToken,
    String username,
    long expiresIn
) {}
