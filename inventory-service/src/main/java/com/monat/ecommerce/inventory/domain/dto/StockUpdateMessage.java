package com.monat.ecommerce.inventory.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * RabbitMQ üzerinden geçirilecek ve Batch üzerinden okunan stok mesaj modeli.
 */
public record StockUpdateMessage(
        @NotBlank(message = "SKU cannot be blank")
        String sku,

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @NotBlank(message = "Operation type cannot be blank (e.g. ADD, SET)")
        String operationType, // "ADD" (stok ekle), "SET" (stoğu doğrudan x yap)
        
        String referenceId // Opsiyonel, işlemin izlenebilirliği için
) {
}
