package com.monat.ecommerce.cart.application.service;

import com.monat.ecommerce.cart.application.mapper.CartMapper;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartItemResponse;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.domain.model.Cart;
import com.monat.ecommerce.cart.domain.model.CartItem;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CartApplicationService
 */
@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartApplicationService cartApplicationService;

    private Cart cart;
    private AddToCartRequest addToCartRequest;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId("CART-123");
        cart.setItems(new ArrayList<>());
        cart.setCreatedAt(java.time.LocalDateTime.now());
        cart.setUpdatedAt(java.time.LocalDateTime.now());
 
        ReflectionTestUtils.setField(cartApplicationService, "maxItems", 100);

        addToCartRequest = AddToCartRequest.builder()
                .productId("PROD-001")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void addToCart_NewCart() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.empty());
        when(cartMapper.toItem(any())).thenReturn(CartItem.builder().productId("PROD-001").quantity(2).unitPrice(BigDecimal.valueOf(100.00)).build());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("CART-123").build());
 
        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);
 
        // Then
        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo("CART-123");
        verify(cartRepository, times(1)).findById("CART-123");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
 
    @Test
    void addToCart_ExistingCart() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartMapper.toItem(any())).thenReturn(CartItem.builder().productId("PROD-001").quantity(2).unitPrice(BigDecimal.valueOf(100.00)).build());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        CartItemResponse itemResponse = CartItemResponse.builder().productId("PROD-001").quantity(2).build();
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("CART-123").items(java.util.List.of(itemResponse)).build());
 
        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);
 
        // Then
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        verify(cartRepository, times(1)).findById("CART-123");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
 
    @Test
    void addToCart_UpdateExistingItem() {
        // Given
        CartItem existingItem = CartItem.builder()
                .productId("PROD-001")
                .productName("Test Product")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
        cart.getItems().add(existingItem);
 
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartMapper.toItem(any())).thenReturn(CartItem.builder().productId("PROD-001").quantity(2).unitPrice(BigDecimal.valueOf(100.00)).build());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        CartResponse mockResponse = CartResponse.builder()
                .cartId("CART-123")
                .items(java.util.List.of(CartItemResponse.builder().productId("PROD-001").quantity(3).build()))
                .build();
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(mockResponse);
 
        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);
 
        // Then
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3); // 1 + 2
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
 
    @Test
    void addToCart_ExceedsMaxItems() {
        // Given
        for (int i = 0; i < 100; i++) {
            cart.getItems().add(CartItem.builder()
                    .productId("PROD-" + i)
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .build());
        }
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
 
        AddToCartRequest newItem = AddToCartRequest.builder()
                .productId("PROD-NEW")
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .build();
 
        // When & Then
        assertThatThrownBy(() -> cartApplicationService.addToCart("CART-123", newItem))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reached maximum capacity");
 
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_Found() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("CART-123").build());

        // When
        CartResponse response = cartApplicationService.getCart("CART-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo("CART-123");
        verify(cartRepository, times(1)).findById("CART-123");
    }

    @Test
    void getCart_NotFound_ReturnsEmpty() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.empty());
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("CART-123").items(java.util.Collections.emptyList()).build());

        // When
        CartResponse response = cartApplicationService.getCart("CART-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo("CART-123");
        assertThat(response.items()).isEmpty();
    }

    @Test
    void updateQuantity_Success() {
        // Given
        CartItem item = CartItem.builder()
                .productId("PROD-001")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
        cart.getItems().add(item);

        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        CartResponse mockResponse = CartResponse.builder()
                .cartId("CART-123")
                .items(List.of(CartItemResponse.builder().productId("PROD-001").quantity(10).build()))
                .build();
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(mockResponse);

        // When
        CartResponse response = cartApplicationService.updateItemQuantity("CART-123", "PROD-001", 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.items().get(0).quantity()).isEqualTo(10);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void removeItem_Success() {
        // Given
        CartItem item = CartItem.builder()
                .productId("PROD-001")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();
        cart.getItems().add(item);

        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("CART-123").items(java.util.Collections.emptyList()).build());

        // When
        CartResponse response = cartApplicationService.removeItem("CART-123", "PROD-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void clearCart_Success() {
        // Given
        cart.getItems().add(CartItem.builder()
                .productId("PROD-001")
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .build());
 
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
 
        // When
        cartApplicationService.clearCart("CART-123");
 
        // Then
        verify(cartRepository, times(1)).save(any(Cart.class));
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void mergeCart_Success() {
        // Given
        Cart sourceCart = Cart.builder()
                .cartId("SOURCE-CART")
                .userId(null)
                .items(new ArrayList<>())
                .build();
        sourceCart.getItems().add(CartItem.builder()
                .productId("PROD-001")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(50.00))
                .build());

        Cart targetCart = Cart.builder()
                .cartId("TARGET-CART")
                .userId("1")
                .items(new ArrayList<>())
                .build();

        when(cartRepository.findById("SOURCE-CART")).thenReturn(Optional.of(sourceCart));
        when(cartRepository.findById("TARGET-CART")).thenReturn(Optional.of(targetCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(targetCart);
        doNothing().when(cartRepository).delete("SOURCE-CART");
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(CartResponse.builder().cartId("TARGET-CART").items(List.of(CartItemResponse.builder().build())).build());

        // When
        CartResponse response = cartApplicationService.mergeCart("SOURCE-CART", "TARGET-CART");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartRepository, times(1)).delete("SOURCE-CART");
    }
}
