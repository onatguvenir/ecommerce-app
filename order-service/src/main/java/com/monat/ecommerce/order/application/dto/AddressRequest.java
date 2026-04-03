package com.monat.ecommerce.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AddressRequest(
        @NotBlank
        String street,
        @NotBlank
        String city,
        String state,
        String postalCode,
        @NotBlank
        String country
) {
}
