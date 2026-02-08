package com.monat.ecommerce.order.integration;

import com.monat.ecommerce.order.OrderServiceApplication;
import com.monat.ecommerce.order.application.dto.CreateOrderRequest;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Order Service using Testcontainers
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_Success() throws Exception {
        // Given
        String orderJson = """
                {
                  "userId": 1,
                  "items": [
                    {
                      "productId": "PROD-001",
                      "quantity": 2,
                      "price": 99.99
                    }
                  ],
                  "shippingAddress": {
                    "street": "123 Main St",
                    "city": "New York",
                    "state": "NY",
                    "zipCode": "10001",
                    "country": "USA"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.totalAmount", is(199.98)));
    }

    @Test
    void getOrder_Success() throws Exception {
        // Given - Create an order first
        String orderJson = """
                {
                  "userId": 1,
                  "items": [{
                    "productId": "PROD-001",
                    "quantity": 1,
                    "price": 50.00
                  }],
                  "shippingAddress": {
                    "street": "456 Elm St",
                    "city": "Boston",
                    "state": "MA",
                    "zipCode": "02101",
                    "country": "USA"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract order ID from response
        Long orderId = 1L; // Simplified for example

        // When & Then
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.data.status", notNullValue()));
    }

    @Test
    void getOrder_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserOrders_ReturnsOrderList() throws Exception {
        // Given - Create multiple orders for user 1
        createTestOrder(1L, "PROD-001", 1, 100.00);
        createTestOrder(1L, "PROD-002", 2, 50.00);

        // When & Then
        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    private void createTestOrder(Long userId, String productId, int quantity, double price) throws Exception {
        String orderJson = String.format("""
                {
                  "userId": %d,
                  "items": [{
                    "productId": "%s",
                    "quantity": %d,
                    "price": %.2f
                  }],
                  "shippingAddress": {
                    "street": "123 Test St",
                    "city": "Test City",
                    "state": "TS",
                    "zipCode": "12345",
                    "country": "USA"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """, userId, productId, quantity, price);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson));
    }
}
