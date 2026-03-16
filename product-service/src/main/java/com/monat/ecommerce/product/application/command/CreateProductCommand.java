package com.monat.ecommerce.product.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CQRS Command: Yeni bir ürün oluşturma isteğini temsil eder.
 * <p>
 * Command Pattern: Bir işlemi (mutation) kapsülleyen immutable nesne.
 * Record kullanımı sayesinde boilerplate kod azaltılmış, immutability
 * garantilenmiştir.
 * Bean Validation anotasyonları ile gelen veri handler'a ulaşmadan doğrulanır.
 * </p>
 */
public record CreateProductCommand(

        @NotBlank(message = "Product ID is required") String productId,

        @NotBlank(message = "Product name is required") @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters") String name,

        @NotBlank(message = "Description is required") @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters") String description,

        @NotBlank(message = "Category is required") String category,

        String brand,

        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,

        String currency,

        List<String> images,

        List<String> tags,

        // Specifications
        String weight,
        String dimensions,
        String color,
        String material,
        Map<String, String> additionalSpecs) {
    /**
     * Compact constructor: currency için default değer ataması.
     * Record'larda canonical constructor'ı override etmek yerine
     * compact constructor kullanmak daha idiomatiktir.
     */
    public CreateProductCommand {
        currency = (currency == null || currency.isBlank()) ? "USD" : currency;
    }
}
