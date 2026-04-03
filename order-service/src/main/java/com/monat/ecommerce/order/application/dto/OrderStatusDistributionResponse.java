package com.monat.ecommerce.order.application.dto;

import java.math.BigDecimal;

public record OrderStatusDistributionResponse(
        String status,
        long orderCount,
        BigDecimal totalSales,
        BigDecimal sharePercentage
) {
}
