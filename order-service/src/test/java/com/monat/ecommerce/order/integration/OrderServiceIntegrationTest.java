package com.monat.ecommerce.order.integration;

import com.monat.ecommerce.order.OrderServiceApplication;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Order Service using real PostgreSQL and Kafka containers.
 *
 * <p>Skip Strategy:
 * {@code @ExtendWith(DockerRequiredExtension.class)} is a JUnit 5 ExecutionCondition
 * evaluated BEFORE all BeforeAllCallbacks (Spring context load, Testcontainers, etc.).
 * When Docker is unavailable, the class is SKIPPED — no containers are started.
 */
@ExtendWith(DockerRequiredExtension.class)
@SpringBootTest(
    classes = OrderServiceApplication.class,
    properties = {
        "application.config.cart-service-url=http://localhost:${wiremock.server.port}"
    }
)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class OrderServiceIntegrationTest {

    private static final String TEST_SCHEMA = "it_order_test";

    // Lazy singletons started in @DynamicPropertySource (Docker already confirmed up).
    private static PostgreSQLContainer<?> postgres;
    private static KafkaContainer kafka;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
            postgres.start();
        }
        if (kafka == null) {
            kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
            kafka.start();
        }

        registry.add("application.datasource.primary.url", postgres::getJdbcUrl);
        registry.add("application.datasource.primary.username", postgres::getUsername);
        registry.add("application.datasource.primary.password", postgres::getPassword);
        registry.add("application.datasource.replica.url", postgres::getJdbcUrl);
        registry.add("application.datasource.replica.username", postgres::getUsername);
        registry.add("application.datasource.replica.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("management.tracing.enabled", () -> "false");
    }

    @AfterAll
    static void stopContainers() {
        if (kafka != null && kafka.isRunning()) {
            kafka.stop();
            kafka = null;
        }
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            postgres = null;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // Stub Cart Service globally for integration tests to prevent Feign timeouts
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/api/v1/carts/.*"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"Ok\",\"data\":{\"cartId\":\"mock-cart\",\"items\":[],\"totalAmount\":0}}")));
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathMatching("/api/v1/carts/.*"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"message\":\"Ok\",\"data\":null}")));
    }

    @Test
    void createOrder_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderJson = String.format(java.util.Locale.US, """
                {
                  "userId": "%s",
                  "items": [
                    {
                      "productId": "PROD-001",
                      "quantity": 2,
                      "unitPrice": 99.99
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
                """, userId);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId", is(userId.toString())))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.totalAmount", is(199.98)));
    }

    @Test
    void getOrder_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        String orderJson = String.format(java.util.Locale.US, """
                {
                  "userId": "%s",
                  "items": [{"productId": "PROD-001", "quantity": 1, "unitPrice": 50.00}],
                  "shippingAddress": {
                    "street": "456 Elm St", "city": "Boston", "state": "MA",
                    "zipCode": "02101", "country": "USA"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """, userId);

        String createResponse = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(orderId)))
                .andExpect(jsonPath("$.data.status", notNullValue()));
    }

    @Test
    void getOrder_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserOrders_ReturnsOrderList() throws Exception {
        UUID userId = UUID.randomUUID();
        createTestOrder(userId, "PROD-001", 1, 100.00);
        createTestOrder(userId, "PROD-002", 2, 50.00);

        mockMvc.perform(get("/api/orders/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(2)));
    }

    private void createTestOrder(UUID userId, String productId, int quantity, double price) throws Exception {
        String orderJson = String.format(java.util.Locale.US, """
                {
                  "userId": "%s",
                  "items": [{"productId": "%s", "quantity": %d, "unitPrice": %.2f}],
                  "shippingAddress": {
                    "street": "123 Test St", "city": "Test City", "state": "TS",
                    "zipCode": "12345", "country": "USA"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """, userId, productId, quantity, price);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson));
    }
}
