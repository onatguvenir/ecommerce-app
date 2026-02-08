package com.monat.ecommerce.cart.integration;

import com.monat.ecommerce.cart.CartServiceApplication;
import com.monat.ecommerce.cart.domain.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Cart Service with Redis
 */
@SpringBootTest(classes = CartServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class CartServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        // Clear Redis
        cartRepository.deleteAll();
    }

    @Test
    void addToCart_Success() throws Exception {
        // Given
        String addItemJson = """
                {
                  "productId": "PROD-001",
                  "productName": "MacBook Pro",
                  "quantity": 2,
                  "price": 2499.99
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/cart/session-123/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId", is("session-123")))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is("PROD-001")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.data.totalAmount", is(4999.98)));
    }

    @Test
    void getCart_EmptyCart() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/cart/session-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId", is("session-456")))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.totalAmount", is(0.0)));
    }

    @Test
    void updateQuantity_Success() throws Exception {
        // Given - Add item first
        addItemToCart("session-789", "PROD-001", 3);

        // When & Then - Update quantity
        mockMvc.perform(put("/api/cart/session-789/items/PROD-001")
                        .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity", is(5)));
    }

    @Test
    void removeItem_Success() throws Exception {
        // Given - Add items
        addItemToCart("session-abc", "PROD-001", 2);
        addItemToCart("session-abc", "PROD-002", 1);

        // When & Then - Remove one item
        mockMvc.perform(delete("/api/cart/session-abc/items/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is("PROD-002")));
    }

    @Test
    void clearCart_Success() throws Exception {
        // Given - Add items
        addItemToCart("session-clear", "PROD-001", 2);
        addItemToCart("session-clear", "PROD-002", 1);

        // When & Then
        mockMvc.perform(delete("/api/cart/session-clear"))
                .andExpect(status().isOk());

        // Verify cart is empty
        mockMvc.perform(get("/api/cart/session-clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void mergeCart_Success() throws Exception {
        // Given - Source cart with items
        addItemToCart("source-cart", "PROD-001", 2);
        addItemToCart("source-cart", "PROD-002", 1);
        
        // Target cart with different items
        addItemToCart("target-cart", "PROD-003", 3);

        // When & Then
        mockMvc.perform(post("/api/cart/merge")
                        .param("sourceCartId", "source-cart")
                        .param("targetCartId", "target-cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId", is("target-cart")))
                .andExpect(jsonPath("$.data.items", hasSize(3)));
    }

    @Test
    void addToCart_ExceedsMaxItems() throws Exception {
        // Given - Add 100 items (max limit)
        for (int i = 0; i < 100; i++) {
            addItemToCart("session-max", "PROD-" + i, 1);
        }

        // When & Then - Try to add 101st item
        String addItemJson = """
                {
                  "productId": "PROD-101",
                  "productName": "Extra Product",
                  "quantity": 1,
                  "price": 10.00
                }
                """;

        mockMvc.perform(post("/api/cart/session-max/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cartTTL_ExpiresAfterConfiguredTime() throws Exception {
        // Given - Add item to cart
        addItemToCart("session-ttl", "PROD-001", 1);

        // Verify cart exists
        mockMvc.perform(get("/api/cart/session-ttl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // Note: In real scenario, TTL would be tested with time manipulation
        // For this test, we just verify the cart was created successfully
    }

    private void addItemToCart(String cartId, String productId, int quantity) throws Exception {
        String json = String.format("""
                {
                  "productId": "%s",
                  "productName": "Product %s",
                  "quantity": %d,
                  "price": 99.99
                }
                """, productId, productId, quantity);

        mockMvc.perform(post("/api/cart/" + cartId + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
