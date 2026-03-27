package com.monat.ecommerce.user.application.dto;

import lombok.Builder;

/**
 * Response DTO for successful authentication.
 */
@Builder
public record AuthResponse(
    String accessToken,
    String username,
    long expiresIn
) {}
