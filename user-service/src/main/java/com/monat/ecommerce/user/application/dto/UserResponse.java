package com.monat.ecommerce.user.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user response
 */
@Builder
public record UserResponse(
    UUID id,
    String email,
    String username,
    String firstName,
    String lastName,
    String phone,
    String status,
    List<AddressResponse> addresses,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
