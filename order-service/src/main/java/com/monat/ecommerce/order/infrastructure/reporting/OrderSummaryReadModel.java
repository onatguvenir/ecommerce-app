package com.monat.ecommerce.order.infrastructure.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryReadModel(
        UUID id,
        String orderNumber,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        String currency,
        String paymentReference,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
