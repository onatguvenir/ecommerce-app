package com.monat.ecommerce.order.infrastructure.controller;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.DailySalesReportResponse;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.application.dto.OrderStatusDistributionResponse;
import com.monat.ecommerce.order.application.service.OrderApplicationService;
import com.monat.ecommerce.order.domain.model.OrderStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Order Management.
 * 
 * Educational Note:
 * This controller serves as the entry point for order creation and 
 * tracking. All order creation flows go through the OrderApplicationService 
 * to ensure consistency via the Outbox Pattern.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for order creation and management")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderApplicationService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order created successfully"));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable(name = "orderId") UUID orderId) {
        OrderResponse response = orderApplicationService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable(name = "orderNumber") String orderNumber) {
        OrderResponse response = orderApplicationService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user order history from read replica")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getUserOrders(
            @PathVariable(name = "userId") UUID userId,
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "20", name = "size") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderResponse> response = orderApplicationService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List orders from read replica")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> listOrders(
            @RequestParam(required = false, name = "status") OrderStatus status,
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "20", name = "size") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderResponse> response = orderApplicationService.listOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel a pending order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable(name = "orderId") UUID orderId,
            @RequestParam(required = false, name = "reason") String reason) {

        OrderResponse response = orderApplicationService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Order cancelled successfully"));
    }

    @GetMapping("/reports/daily-sales")
    @Operation(summary = "Get daily sales report from materialized view")
    public ResponseEntity<ApiResponse<List<DailySalesReportResponse>>> getDailySalesReport(
            @RequestParam(required = false, name = "startDate") LocalDate startDate,
            @RequestParam(required = false, name = "endDate") LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success(orderApplicationService.getDailySalesReport(startDate, endDate)));
    }

    @GetMapping("/reports/status-distribution")
    @Operation(summary = "Get order status distribution from materialized view")
    public ResponseEntity<ApiResponse<List<OrderStatusDistributionResponse>>> getOrderStatusDistribution() {
        return ResponseEntity.ok(ApiResponse.success(orderApplicationService.getOrderStatusDistribution()));
    }
}
