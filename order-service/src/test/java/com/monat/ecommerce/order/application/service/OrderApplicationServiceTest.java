package com.monat.ecommerce.order.application.service;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.order.application.dto.AddressRequest;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.application.dto.OrderResponse;
import com.monat.ecommerce.order.application.dto.OrderItemRequest;
import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.model.dto.CartDto;
import com.monat.ecommerce.order.domain.model.dto.CartItemDto;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.service.OrderSagaOrchestrator;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        @Mock
        private CartClient cartClient;

        @Mock
        private com.monat.ecommerce.order.application.dto.OrderMapper orderMapper;

        @Mock
        private OrderSagaOrchestrator sagaOrchestrator;

        @InjectMocks
        private OrderApplicationService orderApplicationService;

        private CreateOrderRequest createOrderRequest;
        private Order order;
        private UUID orderId = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                createOrderRequest = CreateOrderRequest.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .shippingAddress(AddressRequest.builder()
                                                .street("123 Main St")
                                                .city("New York")
                                                .state("NY")
                                                .postalCode("10001")
                                                .country("USA")
                                                .build())
                                .build();

                order = Order.builder()
                                .id(orderId)
                                .userId(userId)
                                .status(OrderStatus.PENDING)
                                .totalAmount(BigDecimal.valueOf(100.00))
                                .items(new ArrayList<>())
                                .build();
        }

        @Test
        void createOrder_Success() {
                // Given
                createOrderRequest.setItems(List.of(OrderItemRequest.builder()
                                .productId("PROD-1")
                                .quantity(1)
                                .unitPrice(BigDecimal.valueOf(100.00))
                                .build()));
                when(orderMapper.toOrderItem(any(OrderItemRequest.class)))
                                .thenReturn(com.monat.ecommerce.order.domain.model.OrderItem.builder()
                                                .productId("PROD-1")
                                                .quantity(1)
                                                .unitPrice(BigDecimal.valueOf(100.00))
                                                .build());
                when(orderMapper.toShippingAddress(any(AddressRequest.class)))
                                .thenReturn(com.monat.ecommerce.order.domain.model.ShippingAddress.builder()
                                                .street("123 Main St")
                                                .city("New York")
                                                .state("NY")
                                                .postalCode("10001")
                                                .country("USA")
                                                .build());
                when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                                .id(orderId)
                                .userId(userId)
                                .status(OrderStatus.PENDING.name())
                                .build());
                when(orderRepository.save(any(Order.class))).thenReturn(order);

                // sagaOrchestrator.executeOrderSaga is void, so we don't need to mock return
                // but it's called in a thread.
                // Since it's a separate thread we can't easily verify it without more complex
                // setup, but it won't break the test.

                // When
                OrderResponse response = orderApplicationService.createOrder(createOrderRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
                verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        void createOrder_FromCart_Success() {
                // Given
                String cartId = "cart-123";
                createOrderRequest.setCartId(cartId);

                CartDto cartDto = CartDto.builder()
                                .cartId(cartId)
                                .items(List.of(CartItemDto.builder()
                                                .productId("PROD-1")
                                                .quantity(1)
                                                .unitPrice(BigDecimal.valueOf(100.00))
                                                .build()))
                                .build();

                when(cartClient.getCart(cartId)).thenReturn(ApiResponse.success(cartDto));
                when(orderMapper.toOrderItem(any(OrderItemRequest.class)))
                                .thenReturn(com.monat.ecommerce.order.domain.model.OrderItem.builder()
                                                .productId("PROD-1")
                                                .quantity(1)
                                                .unitPrice(BigDecimal.valueOf(100.00))
                                                .build());
                when(orderMapper.toShippingAddress(any(AddressRequest.class)))
                                .thenReturn(com.monat.ecommerce.order.domain.model.ShippingAddress.builder()
                                                .street("123 Main St")
                                                .city("New York")
                                                .state("NY")
                                                .postalCode("10001")
                                                .country("USA")
                                                .build());
                when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                                .id(orderId)
                                .userId(userId)
                                .status(OrderStatus.PENDING.name())
                                .build());
                when(orderRepository.save(any(Order.class))).thenReturn(order);

                // When
                OrderResponse response = orderApplicationService.createOrder(createOrderRequest);

                // Then
                assertThat(response).isNotNull();
                verify(cartClient, times(1)).getCart(cartId);
                verify(cartClient, times(1)).deleteCart(cartId);
                verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        void getOrder_Found() {
                // Given
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
                when(orderMapper.toOrderResponse(order)).thenReturn(OrderResponse.builder()
                                .id(orderId)
                                .userId(userId)
                                .status(OrderStatus.PENDING.name())
                                .build());

                // When
                OrderResponse response = orderApplicationService.getOrderById(orderId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(orderId);
                assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING.name());
                verify(orderRepository, times(1)).findByIdWithItems(orderId);
        }

        @Test
        void getOrder_NotFound() {
                // Given
                UUID randomId = UUID.randomUUID();
                when(orderRepository.findByIdWithItems(randomId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> orderApplicationService.getOrderById(randomId))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Order", randomId.toString());

                verify(orderRepository, times(1)).findByIdWithItems(randomId);
        }

        @Test
        void getUserOrders_ReturnsOrderList() {
                // Given
                Pageable pageable = PageRequest.of(0, 10);

                when(orderRepository.findByUserId(userId, 0, 10)).thenReturn(List.of(order));
                when(orderRepository.countByUserId(userId)).thenReturn(1L);

                when(orderMapper.toOrderResponseList(anyList())).thenReturn(List.of(OrderResponse.builder()
                                .id(orderId)
                                .userId(userId)
                                .status(OrderStatus.PENDING.name())
                                .build()));

                // When
                PagedResponse<OrderResponse> orders = orderApplicationService.getUserOrders(userId, pageable);

                // Then
                assertThat(orders).isNotNull();
                assertThat(orders.getContent()).hasSize(1);
                verify(orderRepository, times(1)).findByUserId(userId, 0, 10);
                verify(orderRepository, times(1)).countByUserId(userId);
        }
}
