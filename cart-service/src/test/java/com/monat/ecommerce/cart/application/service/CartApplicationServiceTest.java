package com.monat.ecommerce.cart.application.service;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
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

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @InjectMocks
    private CartApplicationService cartApplicationService;

    private Cart cart;
    private AddToCartRequest addToCartRequest;

    @BeforeEach
    void setUp() {
        cart = Cart.builder()
                .cartId("CART-123")
                .userId(null)
                .items(new ArrayList<>())
                .build();

        addToCartRequest = AddToCartRequest.builder()
                .productId("PROD-001")
                .productName("Test Product")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(99.99))
                .build();
    }

    @Test
    void addToCart_NewCart() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo("CART-123");
        verify(cartRepository, times(1)).findById("CART-123");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addToCart_ExistingCart() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
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
                .unitPrice(BigDecimal.valueOf(99.99))
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        CartResponse response = cartApplicationService.addToCart("CART-123", addToCartRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3); // 1 + 2
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
                .hasMessageContaining("maximum number of items");

        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_Found() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));

        // When
        CartResponse response = cartApplicationService.getCart("CART-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo("CART-123");
        verify(cartRepository, times(1)).findById("CART-123");
    }

    @Test
    void getCart_NotFound_ReturnsEmpty() {
        // Given
        when(cartRepository.findById("CART-123")).thenReturn(Optional.empty());

        // When
        CartResponse response = cartApplicationService.getCart("CART-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo("CART-123");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void updateQuantity_Success() {
        // Given
        CartItem item = CartItem.builder()
                .productId("PROD-001")
                .quantity(5)
                .unitPrice(BigDecimal.valueOf(99.99))
                .build();
        cart.getItems().add(item);

        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        CartResponse response = cartApplicationService.updateItemQuantity("CART-123", "PROD-001", 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(10);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void removeItem_Success() {
        // Given
        CartItem item = CartItem.builder()
                .productId("PROD-001")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(99.99))
                .build();
        cart.getItems().add(item);

        when(cartRepository.findById("CART-123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        CartResponse response = cartApplicationService.removeItem("CART-123", "PROD-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
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
        doNothing().when(cartRepository).delete(anyString());

        // When
        cartApplicationService.clearCart("CART-123");

        // Then
        verify(cartRepository, times(1)).delete("CART-123");
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

        // When
        CartResponse response = cartApplicationService.mergeCart("SOURCE-CART", "TARGET-CART");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartRepository, times(1)).delete("SOURCE-CART");
    }
}
