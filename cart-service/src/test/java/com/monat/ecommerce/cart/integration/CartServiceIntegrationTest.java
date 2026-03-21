package com.monat.ecommerce.cart.integration;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.service.CartApplicationService;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cart Service Integration Test.
 * 
 * Educational Note:
 * This test verifies the end-to-end flow of the shopping cart using Redis.
 * It ensures that cart items are correctly persisted and retrieved, and 
 * validates the 'Cart Merge' logic used when an anonymous user logs in.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Cart Service Integration Tests (Redis)")
class CartServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired
    private CartApplicationService cartService;

    @Autowired
    private CartRepository cartRepository;

    private final String userId = "user-123";
    private final String anonId = "anon-456";

    @BeforeEach
    void setUp() {
        cartRepository.delete(userId);
        cartRepository.delete(anonId);
    }

    @Test
    @DisplayName("Should add items to cart and persist in Redis")
    void shouldAddItemAndPersist() {
        // Arrange
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("PROD-1")
                .productName("Product 1")
                .unitPrice(BigDecimal.valueOf(100.0))
                .quantity(2)
                .build();

        // Act
        CartResponse response = cartService.addToCart(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo(userId);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount().doubleValue()).isEqualTo(200.0);

        // Verify it exists in Redis
        CartResponse fetched = cartService.getCart(userId);
        assertThat(fetched.items()).hasSize(1);
    }

    @Test
    @DisplayName("Should merge anonymous cart into user cart")
    void shouldMergeCarts() {
        // Arrange: Add item to anonymous cart
        cartService.addToCart(anonId, AddToCartRequest.builder()
                .productId("PROD-1")
                .productName("Product 1")
                .unitPrice(BigDecimal.valueOf(100.0))
                .quantity(1)
                .build());
        
        // Arrange: Add different item to user cart
        cartService.addToCart(userId, AddToCartRequest.builder()
                .productId("PROD-2")
                .productName("Product 2")
                .unitPrice(BigDecimal.valueOf(50.0))
                .quantity(2)
                .build());

        // Act: Merge
        CartResponse mergedResponse = cartService.mergeCart(anonId, userId);

        // Assert
        assertThat(mergedResponse.items()).hasSize(2);
        assertThat(mergedResponse.totalAmount().doubleValue()).isEqualTo(200.0); // 100*1 + 50*2

        // Anonymous cart should be deleted
        assertThat(cartRepository.findById(anonId)).isEmpty();
    }
}
