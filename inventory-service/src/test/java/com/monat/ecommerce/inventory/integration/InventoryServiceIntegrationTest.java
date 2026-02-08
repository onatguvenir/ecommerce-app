package com.monat.ecommerce.inventory.integration;

import com.monat.ecommerce.inventory.InventoryServiceApplication;
import com.monat.ecommerce.inventory.domain.model.InventoryItem;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Inventory Service with PostgreSQL and Redis
 */
@SpringBootTest(classes = InventoryServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class InventoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void checkStock_Success() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .productId("PROD-001")
                .quantity(100)
                .reservedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);

        // When & Then
        mockMvc.perform(get("/api/inventory/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId", is("PROD-001")))
                .andExpect(jsonPath("$.data.quantity", is(100)))
                .andExpect(jsonPath("$.data.availableQuantity", is(100)));
    }

    @Test
    void reserveStock_Success() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .productId("PROD-001")
                .quantity(100)
                .reservedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);

        String reserveJson = """
                {
                  "productId": "PROD-001",
                  "quantity": 10,
                  "orderId": "ORDER-123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserveJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify stock was reserved
        mockMvc.perform(get("/api/inventory/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity", is(90)));
    }

    @Test
    void reserveStock_InsufficientStock() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .productId("PROD-001")
                .quantity(5)
                .reservedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);

        String reserveJson = """
                {
                  "productId": "PROD-001",
                  "quantity": 10,
                  "orderId": "ORDER-123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserveJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void releaseReservation_Success() throws Exception {
        // Given - Create and reserve stock first
        InventoryItem item = InventoryItem.builder()
                .productId("PROD-001")
                .quantity(100)
                .reservedQuantity(10)
                .version(0L)
                .build();
        inventoryRepository.save(item);

        String releaseJson = """
                {
                  "productId": "PROD-001",
                  "quantity": 10,
                  "orderId": "ORDER-123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(releaseJson))
                .andExpect(status().isOk());

        // Verify reservation was released
        mockMvc.perform(get("/api/inventory/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity", is(100)));
    }

    @Test
    void caching_WorksCorrectly() throws Exception {
        // Given
        InventoryItem item = InventoryItem.builder()
                .productId("PROD-001")
                .quantity(100)
                .reservedQuantity(0)
                .version(0L)
                .build();
        inventoryRepository.save(item);

        // First call - cache miss
        mockMvc.perform(get("/api/inventory/PROD-001"))
                .andExpect(status().isOk());

        // Second call - should hit cache
        mockMvc.perform(get("/api/inventory/PROD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity", is(100)));

        // Verify cache contains the item
        Object cached = redisTemplate.opsForValue().get("inventory:PROD-001");
        assertThat(cached).isNotNull();
    }
}
