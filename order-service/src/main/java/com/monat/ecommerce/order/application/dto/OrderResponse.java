package com.monat.ecommerce.order.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID userId,
    String status,
    BigDecimal totalAmount,
    String currency,
    List<OrderItemResponse> items,
    AddressResponse shippingAddress,
    String paymentReference,
    String cancellationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
