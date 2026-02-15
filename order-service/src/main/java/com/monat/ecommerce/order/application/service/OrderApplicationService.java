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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
 * @Service indicates that this class is a "Service" component containing
 *          business logic.
 * 
 * @Transactional ensures that local database operations (saving the order) are
 *                atomic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final com.monat.ecommerce.order.infrastructure.client.CartClient cartClient;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        List<OrderItemRequest> orderItems = request.getItems();

        // If cartId is present, fetch items from cart
        if (request.getCartId() != null && !request.getCartId().isBlank()) {
            log.info("Fetching items from cart: {}", request.getCartId());
            com.monat.ecommerce.common.dto.ApiResponse<com.monat.ecommerce.order.domain.model.dto.CartDto> cartResponse = cartClient
                    .getCart(request.getCartId());

            if (cartResponse != null && cartResponse.getData() != null) {
                orderItems = cartResponse.getData().getItems().stream()
                        .map(item -> OrderItemRequest.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList();
            }
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Calculate total amount
        BigDecimal totalAmount = orderItems.stream()
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
        for (OrderItemRequest itemReq : orderItems) {
            OrderItem item = orderMapper.toOrderItem(itemReq);
            item.calculateSubtotal();
            order.addItem(item);
        }

        // Save order
        order = orderRepository.save(order);
        log.info("Order created with ID: {} and number: {}", order.getId(), order.getOrderNumber());

        // Delete cart if used
        if (request.getCartId() != null && !request.getCartId().isBlank()) {
            try {
                cartClient.deleteCart(request.getCartId());
                log.info("Cart deleted: {}", request.getCartId());
            } catch (Exception e) {
                log.warn("Failed to delete cart: {}", request.getCartId(), e);
                // Don't fail order creation if cart deletion fails
            }
        }

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
