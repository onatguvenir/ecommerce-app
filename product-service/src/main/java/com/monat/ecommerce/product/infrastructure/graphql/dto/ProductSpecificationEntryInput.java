package com.monat.ecommerce.product.infrastructure.graphql.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductSpecificationEntryInput(
        @NotBlank(message = "Specification key is required")
        String key,
        @NotBlank(message = "Specification value is required")
        String value) {
}
