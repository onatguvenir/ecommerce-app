package com.monat.ecommerce.product.application.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record CreateProductRequest(
    @NotBlank(message = "Product ID is required")
    String productId,

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    String name,

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    String description,

    @NotBlank(message = "Category is required")
    String category,

    String brand,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    String currency,

    List<String> images,

    List<String> tags,

    // Specifications
    String weight,
    String dimensions,
    String color,
    String material,
    Map<String, String> additionalSpecs
) {
    public CreateProductRequest {
        if (currency == null) currency = "USD";
    }
}
