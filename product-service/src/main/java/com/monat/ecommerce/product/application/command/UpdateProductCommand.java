package com.monat.ecommerce.product.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CQRS Command: Mevcut bir ürünü güncelleme isteğini temsil eder.
 * <p>
 * productId path variable'dan, geri kalan alanlar request body'den gelir.
 * Immutable record olduğu için thread-safe'dir.
 * </p>
 */
public record UpdateProductCommand(

        @NotBlank(message = "Product ID is required") String productId,

        @NotBlank(message = "Product name is required") @Size(min = 3, max = 200) String name,

        @NotBlank(message = "Description is required") @Size(min = 10, max = 2000) String description,

        @NotBlank(message = "Category is required") String category,

        String brand,

        @NotNull(message = "Price is required") @DecimalMin(value = "0.01") BigDecimal price,

        String currency,

        List<String> images,

        List<String> tags,

        String weight,
        String dimensions,
        String color,
        String material,
        Map<String, String> additionalSpecs) {
}
