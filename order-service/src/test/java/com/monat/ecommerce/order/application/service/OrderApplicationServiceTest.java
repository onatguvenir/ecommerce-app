package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderApplicationService
 */
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private CreateOrderRequest createOrderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        createOrderRequest = CreateOrderRequest.builder()
                .userId(1L)
                .items(new ArrayList<>())
                .shippingAddress(CreateOrderRequest.ShippingAddressRequest.builder()
                        .street("123 Main St")
                        .city("New York")
                        .state("NY")
                        .zipCode("10001")
                        .country("USA")
                        .build())
                .paymentMethod("CREDIT_CARD")
                .build();

        order = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.00))
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void createOrder_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderResponse response = orderApplicationService.createOrder(createOrderRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrder_Found() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        OrderResponse response = orderApplicationService.getOrder(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrder_NotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.getOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
        
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void updateOrderStatus_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderResponse response = orderApplicationService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED.name());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getUserOrders_ReturnsOrderList() {
        // Given
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

        // When
        List<OrderResponse> orders = orderApplicationService.getUserOrders(1L);

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo(1L);
        verify(orderRepository, times(1)).findByUserId(1L);
    }
}
