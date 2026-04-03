package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.order.application.dto.AddressRequest;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.OrderMapper;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderItem;
import com.monat.ecommerce.order.domain.model.dto.CartDto;
import com.monat.ecommerce.order.domain.model.dto.CartItemDto;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;
import com.monat.ecommerce.order.infrastructure.reporting.OrderAnalyticsRepository;

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
    private CartClient cartClient;

    @Mock
    private OrderMetrics orderMetrics;

    @Mock
    private OrderAnalyticsRepository orderAnalyticsRepository;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @BeforeEach
    void setUp() {
        when(orderMetrics.orderCreationTimer()).thenReturn(
                io.micrometer.core.instrument.Timer.builder("test.order.creation")
                        .register(new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("Should create order from cart successfully and initiate Saga")
    void shouldCreateOrderFromCartSuccessfully() {
        // Arrange
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
        verify(cartClient, times(1)).deleteCart("cart-123");
        verify(orderRepository, times(1)).save(any());

        // Note: sagaOrchestrator is called in a separate thread. 
        // In a real project, we might use a spy or Captor with Awaitility 
        // or refactor to use an ExecutorService that can be synchronous in tests.
    }

    @Test
    @DisplayName("Should throw exception when trying to create order with empty cart")
    void shouldFailWhenCartIsEmpty() {
        // Arrange
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
