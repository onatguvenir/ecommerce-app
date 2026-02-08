package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.order.application.dto.*;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Calculate total amount
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .totalAmount(totalAmount)
                .currency("USD")
                .shippingAddress(orderMapper.toShippingAddress(request.getShippingAddress()))
                .build();

        // Add items
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = orderMapper.toOrderItem(itemReq);
            item.calculateSubtotal();
            order.addItem(item);
        }

        // Save order
        order = orderRepository.save(order);
        log.info("Order created with ID: {} and number: {}", order.getId(), order.getOrderNumber());

        // Execute Saga asynchronously  
        Order finalOrder = order;
        new Thread(() -> sagaOrchestrator.executeOrderSaga(finalOrder)).start();

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        log.debug("Fetching order by ID: {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.debug("Fetching order by number: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with number: " + orderNumber));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);

        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        return PagedResponse.<OrderResponse>builder()
                .content(orderMapper.toOrderResponseList(orderPage.getContent()))
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
