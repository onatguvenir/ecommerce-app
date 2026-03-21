package com.monat.ecommerce.inventory.application.dto;
 
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
 
@Builder
public record StockReservationRequest(
    @NotBlank(message = "Product ID is required")
    String productId,
 
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,
 
    @NotBlank(message = "Order ID is required")
    String orderId
) {}
