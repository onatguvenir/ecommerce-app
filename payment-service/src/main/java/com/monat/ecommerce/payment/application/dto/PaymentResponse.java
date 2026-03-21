package com.monat.ecommerce.payment.application.dto;
 
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
 
@Builder
public record PaymentResponse(
    UUID id,
    String orderId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String status,
    String transactionId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
