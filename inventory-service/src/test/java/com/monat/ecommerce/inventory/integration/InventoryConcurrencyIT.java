package com.monat.ecommerce.inventory.integration;

import com.monat.ecommerce.inventory.application.dto.StockReservationRequest;
import com.monat.ecommerce.inventory.application.service.InventoryApplicationService;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Inventory Concurrency Integration Test.
 * 
 * Educational Note:
 * This test demonstrates how Pessimistic Write Locking (@Lock(LockModeType.PESSIMISTIC_WRITE))
 * prevents race conditions in a high-concurrency environment. 
 * By forcing threads to wait for the lock, we ensure that stock is never 
 * over-reserved even when multiple orders hit the system at the exact same millisecond.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Inventory Concurrency & Locking Tests")
class InventoryConcurrencyIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private InventoryApplicationService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private final String productId = "CONC-PROD-999";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure clean state for integration tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        inventoryService.addStock(productId, 10); // Start with 10 items in stock
    }

    @Test
    @DisplayName("Should accurately handle 50 concurrent reservation attempts for 10 items")
    void shouldHandleConcurrentReservations() throws InterruptedException {
        int totalRequests = 50;
        int itemsInStock = 10;
        
        ExecutorService executorService = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // Act: Fire 50 concurrent requests
        for (int i = 0; i < totalRequests; i++) {
            executorService.execute(() -> {
                try {
                    // Each request tries to reserve 1 item
                    inventoryService.reserveStock(new StockReservationRequest(productId, 1, "ORDER-ANY"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Assert: 
        // With 10 items in stock, EXACTLY 10 should succeed and 40 should fail 
        // if locking is working correctly.
        assertThat(successCount.get()).isEqualTo(itemsInStock);
        assertThat(failureCount.get()).isEqualTo(totalRequests - itemsInStock);

        // Final available stock must be 0
        assertThat(inventoryService.getInventory(productId).availableQuantity()).isEqualTo(0);
        assertThat(inventoryService.getInventory(productId).reservedQuantity()).isEqualTo(10);
    }
}
