package com.monat.ecommerce.order.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesReportResponse(
        LocalDate salesDate,
        String status,
        String currency,
        long orderCount,
        long uniqueCustomers,
        BigDecimal totalSales,
        BigDecimal averageOrderValue
) {
}
