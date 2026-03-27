package com.monat.ecommerce.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request DTO for user login.
 */
@Builder
public record AuthRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password
) {}
