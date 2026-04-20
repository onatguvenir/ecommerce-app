package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateAddressRequest(
    AddressType addressType,

    @NotBlank(message = "Street is required")
    String street,

    @NotBlank(message = "City is required")
    String city,

    String state,
    String postalCode,

    @NotBlank(message = "Country is required")
    String country,

    Boolean isDefault
) {}
