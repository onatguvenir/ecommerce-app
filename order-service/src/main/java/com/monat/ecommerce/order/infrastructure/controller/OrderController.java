package com.monat.ecommerce.order.infrastructure.controller;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.application.service.OrderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID orderId) {
        OrderResponse response = orderApplicationService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderApplicationService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders for a user")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getUserOrders(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<OrderResponse> response = orderApplicationService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
