package com.monat.ecommerce.payment.application.dto;
 
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.math.BigDecimal;
 
@Builder
public record ProcessPaymentRequest(
    @NotBlank(message = "Order ID is required")
    String orderId,
 
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,
 
    @NotBlank(message = "Currency is required")
    String currency,
 
    @NotBlank(message = "Payment method is required")
    String paymentMethod,
 
    String idempotencyKey
) {}
