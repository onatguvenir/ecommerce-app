package com.monat.ecommerce.order.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.events.order.OrderCreatedEvent;
import com.monat.ecommerce.order.application.dto.*;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.model.OutboxEvent;
import com.monat.ecommerce.order.domain.model.dto.CartDto;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;
import com.monat.ecommerce.order.infrastructure.reporting.OrderAnalyticsRepository;
import com.monat.ecommerce.order.infrastructure.reporting.OrderReadPage;
import com.monat.ecommerce.order.infrastructure.reporting.OrderSummaryReadModel;

import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order Application Service.
 * <p>
 * This class handles order creation and orchestration.
 * It uses the Saga pattern (via orchestration) to manage distributed
 * transactions across microservices.
 * </p>
 *
 * Coordinator for Order-related business logic.
 *
 * Architecture Note: Transactional Outbox Pattern
 * To ensure 'Exactly-once' processing and avoid dual-write problems:
 * 1. The order is saved to the 'orders' table.
 * 2. An event record is saved to the 'outbox_events' table.
 * Both steps happen in the SAME database transaction.
 * A separate poller then reads from 'outbox_events' and sends to Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final CartClient cartClient;
    private final OrderMetrics orderMetrics;
    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Timer.Sample sample = Timer.start();
        log.info("Creating order for user: {}", request.userId());
        try {
            List<OrderItemRequest> orderItems = request.items();

            if (request.cartId() != null && !request.cartId().isBlank()) {
                log.info("Fetching items from cart: {}", request.cartId());
                ApiResponse<CartDto> cartResponse = cartClient.getCart(request.cartId());

                if (cartResponse != null && cartResponse.data() != null) {
                    orderItems = cartResponse.data().items().stream()
                            .map(item -> OrderItemRequest.builder()
                                    .productId(item.productId())
                                    .quantity(item.quantity())
                                    .unitPrice(item.unitPrice())
                                    .build())
                            .toList();
                }
            }

            if (orderItems == null || orderItems.isEmpty()) {
                orderMetrics.incrementOrderCreationFailure("empty_items");
                throw new IllegalArgumentException("Order must have at least one item");
            }

            String orderNumber = generateOrderNumber();
            BigDecimal totalAmount = orderItems.stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Order order = orderMapper.toOrder(request);
            order.setCreatedAt(LocalDateTime.now());
            order.setOrderNumber(orderNumber);
            order.setTotalAmount(totalAmount);

            for (OrderItemRequest itemReq : orderItems) {
                OrderItem item = orderMapper.toOrderItem(itemReq);
                item.calculateSubtotal();
                order.addItem(item);
            }

            order = orderRepository.save(order);
            log.info("Order created with ID: {} and number: {}", order.getId(), order.getOrderNumber());
            orderMetrics.recordOrderCreated(orderItems.size(), totalAmount.doubleValue(),
                    request.cartId() != null && !request.cartId().isBlank() ? "cart" : "direct");

            publishOrderCreatedEvent(order, orderItems);
            sagaOrchestrator.executeOrderSaga(order.getId(), request.cartId());

            return orderMapper.toOrderResponse(order);
        } catch (RuntimeException ex) {
            orderMetrics.incrementOrderCreationFailure("exception");
            throw ex;
        } finally {
            sample.stop(orderMetrics.orderCreationTimer());
        }
    }

    @Transactional(readOnly = true)
    @Observed(name = "order.lookup", contextualName = "order-get-by-id")
    public OrderResponse getOrderById(UUID orderId) {
        log.debug("Fetching order by ID: {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    @Observed(name = "order.lookup", contextualName = "order-get-by-number")
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.debug("Fetching order by number: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order with number: " + orderNumber));

        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    @Observed(name = "order.lookup", contextualName = "order-get-user-orders")
    public PagedResponse<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);
        OrderReadPage<OrderSummaryReadModel> result = orderAnalyticsRepository.findUserOrderHistory(
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize());
        return toPagedResponse(result, pageable);
    }

    @Transactional(readOnly = true)
    @Observed(name = "order.lookup", contextualName = "order-list")
    public PagedResponse<OrderResponse> listOrders(OrderStatus status, Pageable pageable) {
        log.debug("Listing orders with status: {}", status);
        OrderReadPage<OrderSummaryReadModel> result = orderAnalyticsRepository.findOrders(
                status,
                pageable.getPageNumber(),
                pageable.getPageSize());
        return toPagedResponse(result, pageable);
    }

    @Transactional(readOnly = true)
    public List<DailySalesReportResponse> getDailySalesReport(LocalDate startDate, LocalDate endDate) {
        return orderAnalyticsRepository.findDailySalesReport(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusDistributionResponse> getOrderStatusDistribution() {
        return orderAnalyticsRepository.findOrderStatusDistribution();
    }

    private PagedResponse<OrderResponse> toPagedResponse(
            OrderReadPage<OrderSummaryReadModel> result,
            Pageable pageable
    ) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        long totalElements = result.totalElements();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return PagedResponse.<OrderResponse>builder()
                .content(result.content().stream().map(this::toOrderResponse).toList())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    private OrderResponse toOrderResponse(OrderSummaryReadModel order) {
        return OrderResponse.builder()
                .id(order.id())
                .orderNumber(order.orderNumber())
                .userId(order.userId())
                .status(order.status())
                .totalAmount(order.totalAmount())
                .currency(order.currency())
                .items(List.of())
                .paymentReference(order.paymentReference())
                .cancellationReason(order.cancellationReason())
                .createdAt(order.createdAt())
                .updatedAt(order.updatedAt())
                .build();
    }

    private void publishOrderCreatedEvent(Order order, List<OrderItemRequest> orderItems) {
        try {
            List<OrderCreatedEvent.OrderItemDto> itemDtos = orderItems.stream()
                    .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                            .productId(item.productId())
                            .quantity(item.quantity())
                            .unitPrice(item.unitPrice())
                            .subtotal(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                            .build())
                    .toList();

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId().toString())
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId().toString())
                    .items(itemDtos)
                    .totalAmount(order.getTotalAmount())
                    .currency(order.getCurrency())
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCreated")
                    .payload(payload)
                    .build());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OrderCreatedEvent for order " + order.getId(), e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
