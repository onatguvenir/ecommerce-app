package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.order.application.dto.AddressRequest;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.OrderMapper;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.dto.CartDto;
import com.monat.ecommerce.order.domain.model.dto.CartItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.order.domain.event.OrderSagaStartedEvent;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;
import com.monat.ecommerce.order.infrastructure.reporting.OrderAnalyticsRepository;
import org.springframework.context.ApplicationEventPublisher;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Order Application Service Unit Test.
 * 
 * Educational Note:
 * This unit test focuses on the core orchestration logic of order creation.
 * It uses Mockito to isolate the service from external dependencies like 
 * the database and the Cart microservice, ensuring that the business logic 
 * itself is correct and handles edge cases (e.g., empty cart) properly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Application Service Unit Tests")
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderMetrics orderMetrics;

    @Mock
    private OrderAnalyticsRepository orderAnalyticsRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private io.micrometer.core.instrument.Timer orderCreationTimer() {
        return io.micrometer.core.instrument.Timer.builder("test.order.creation")
                .register(new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Should create order from cart successfully and initiate Saga")
    void shouldCreateOrderFromCartSuccessfully() {
        // Arrange
        when(orderMetrics.orderCreationTimer()).thenReturn(orderCreationTimer());
        UUID userId = UUID.randomUUID();
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(userId)
                .cartId("cart-123")
                .shippingAddress(AddressRequest.builder()
                        .street("123 Main St")
                        .city("Anytown")
                        .country("USA")
                        .build())
                .build();

        CartDto cartDto = CartDto.builder()
                .cartId("cart-123")
                .items(List.of(
                        CartItemDto.builder()
                                .productId("p1")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .build()
                ))
                .build();

        ApiResponse<CartDto> cartApiResponse = ApiResponse.success(cartDto, "Success");
        when(cartClient.getCart("cart-123")).thenReturn(cartApiResponse);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUserId(userId.toString());
        order.setOrderNumber("ORD-TEST-001");

        when(orderMapper.toOrder(any())).thenReturn(order);
        when(orderMapper.toOrderItem(any())).thenReturn(new OrderItem());
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toOrderResponse(any())).thenReturn(OrderResponse.builder().orderNumber("ORD-TEST-001").build());

        // Act
        OrderResponse response = orderApplicationService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-001");

        verify(cartClient, times(1)).getCart("cart-123");
        verify(cartClient, never()).deleteCart(any()); // cart deletion moved to saga completeOrder step
        verify(orderRepository, times(1)).save(any());

        verify(eventPublisher, times(1)).publishEvent(any(OrderSagaStartedEvent.class));
        verify(outboxEventRepository, times(1)).save(any());
        // Cart is deleted inside saga.completeOrder() only when order succeeds.
    }

    @Test
    @DisplayName("Should cancel a PENDING order and publish outbox event")
    void shouldCancelPendingOrderSuccessfully() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(UUID.randomUUID().toString());
        order.setOrderNumber("ORD-TEST-001");
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(orderMapper.toOrderResponse(any())).thenReturn(OrderResponse.builder().orderNumber("ORD-TEST-001").build());

        // Act
        OrderResponse response = orderApplicationService.cancelOrder(orderId, "No longer needed");

        // Assert
        assertThat(response).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo("No longer needed");
        verify(outboxEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should reject cancellation when order is not PENDING")
    void shouldRejectCancellationForNonPendingOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderApplicationService.cancelOrder(orderId, "too late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");

        verify(orderRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order not found for cancellation")
    void shouldThrowNotFoundWhenCancellingNonExistentOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderApplicationService.cancelOrder(orderId, "reason"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when trying to create order with empty cart")
    void shouldFailWhenCartIsEmpty() {
        // Arrange
        when(orderMetrics.orderCreationTimer()).thenReturn(orderCreationTimer());
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(UUID.randomUUID())
                .cartId("empty-cart")
                .build();

        ApiResponse<CartDto> emptyResponse = ApiResponse.success(
                CartDto.builder().cartId("empty-cart").items(Collections.emptyList()).build(), "Success");

        when(cartClient.getCart("empty-cart")).thenReturn(emptyResponse);

        // Act & Assert
        assertThatThrownBy(() -> orderApplicationService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order must have at least one item");
    }
}
