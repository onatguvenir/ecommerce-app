package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

/**
 * DTO for address
 */
@Builder
public record AddressResponse(
    UUID id,
    AddressType addressType,
    String street,
    String city,
    String state,
    String postalCode,
    String country,
    Boolean isDefault
) {}
