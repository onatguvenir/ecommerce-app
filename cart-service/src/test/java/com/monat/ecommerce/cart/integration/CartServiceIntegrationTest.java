package com.monat.ecommerce.cart.integration;

import com.monat.ecommerce.cart.application.dto.AddToCartRequest;
import com.monat.ecommerce.cart.application.dto.CartResponse;
import com.monat.ecommerce.cart.application.service.CartApplicationService;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cart Service Integration Test — verifies end-to-end Redis persistence.
 *
 * <p>Skip Strategy:
 * {@code @ExtendWith(DockerRequiredExtension.class)} evaluates Docker availability
 * BEFORE Spring context loading. If Docker is not reachable the test is SKIPPED.
 */
@ExtendWith(DockerRequiredExtension.class)
@SpringBootTest
@DisplayName("Cart Service Integration Tests (Redis)")
class CartServiceIntegrationTest {

    @SuppressWarnings("rawtypes")
    private static GenericContainer<?> redis;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // DockerRequiredExtension already confirmed Docker is up, so starting is safe.
        if (redis == null) {
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            redis.start();
        }

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
        registry.add("management.tracing.enabled", () -> "false");
        registry.add("server.port", () -> "0");
    }

    @AfterAll
    static void stopContainers() {
        if (redis != null && redis.isRunning()) {
            redis.stop();
            redis = null;
        }
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

        // Verify persistence in Redis
        CartResponse fetched = cartService.getCart(userId);
        assertThat(fetched.items()).hasSize(1);
    }

    @Test
    @DisplayName("Should merge anonymous cart into user cart")
    void shouldMergeCarts() {
        // Arrange: item in anonymous cart
        cartService.addToCart(anonId, AddToCartRequest.builder()
                .productId("PROD-1")
                .productName("Product 1")
                .unitPrice(BigDecimal.valueOf(100.0))
                .quantity(1)
                .build());

        // Arrange: different item in user cart
        cartService.addToCart(userId, AddToCartRequest.builder()
                .productId("PROD-2")
                .productName("Product 2")
                .unitPrice(BigDecimal.valueOf(50.0))
                .quantity(2)
                .build());

        // Act
        CartResponse mergedResponse = cartService.mergeCart(anonId, userId);

        // Assert
        assertThat(mergedResponse.items()).hasSize(2);
        assertThat(mergedResponse.totalAmount().doubleValue()).isEqualTo(200.0); // 100*1 + 50*2

        // Anonymous cart should be deleted after merge
        assertThat(cartRepository.findById(anonId)).isEmpty();
    }
}
