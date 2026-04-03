package com.monat.ecommerce.order.infrastructure.reporting;

import com.monat.ecommerce.order.application.dto.DailySalesReportResponse;
import com.monat.ecommerce.order.application.dto.OrderStatusDistributionResponse;
import com.monat.ecommerce.order.domain.model.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderAnalyticsRepository {

    OrderReadPage<OrderSummaryReadModel> findOrders(OrderStatus status, int page, int size);

    OrderReadPage<OrderSummaryReadModel> findUserOrderHistory(UUID userId, int page, int size);

    List<DailySalesReportResponse> findDailySalesReport(LocalDate startDate, LocalDate endDate);

    List<OrderStatusDistributionResponse> findOrderStatusDistribution();

    void refreshMaterializedViews();
}
