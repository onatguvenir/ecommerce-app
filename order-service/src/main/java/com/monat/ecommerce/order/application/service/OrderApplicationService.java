package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.order.application.dto.*;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.dto.CartDto;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.monat.ecommerce.order.infrastructure.client.CartClient;

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

            if (request.cartId() != null && !request.cartId().isBlank()) {
                try {
                    cartClient.deleteCart(request.cartId());
                    log.info("Cart deleted: {}", request.cartId());
                } catch (Exception e) {
                    log.warn("Failed to delete cart: {}", request.cartId(), e);
                    orderMetrics.incrementOrderCreationFailure("cart_delete_failed");
                }
            }

            Order finalOrder = order;
            new Thread(() -> sagaOrchestrator.executeOrderSaga(finalOrder)).start();

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

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        List<Order> orders = orderRepository.findByUserId(userId, page, size);
        long totalElements = orderRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PagedResponse.<OrderResponse>builder()
                .content(orderMapper.toOrderResponseList(orders))
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page == totalPages - 1)
                .build();
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
