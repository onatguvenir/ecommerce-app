package com.monat.ecommerce.product.application.command;

import jakarta.validation.constraints.NotBlank;

/**
 * CQRS Command: Bir ürünü silme isteğini temsil eder.
 * <p>
 * Sadece business key (productId) taşır; domain katmanı
 * bu key ile entity'yi bulup siler.
 * </p>
 */
public record DeleteProductCommand(

        @NotBlank(message = "Product ID is required") String productId) {
}
