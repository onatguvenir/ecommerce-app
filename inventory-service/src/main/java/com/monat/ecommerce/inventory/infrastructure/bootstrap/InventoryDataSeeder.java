package com.monat.ecommerce.inventory.infrastructure.bootstrap;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds the database with dummy inventory stock.
 * Only runs when 'docker' profile is active and database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class InventoryDataSeeder implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (inventoryRepository.count() > 0) {
            log.info("Inventory already exists. Skipping seeding.");
            return;
        }

        log.info("Seeding inventory...");

        List<String> productIds = Arrays.asList(
                "PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005",
                "PROD-006", "PROD-007", "PROD-008", "PROD-009", "PROD-010",
                "PROD-011", "PROD-012", "PROD-013", "PROD-014", "PROD-015",
                "PROD-016", "PROD-017", "PROD-018", "PROD-019", "PROD-020");

        Random random = new Random();

        for (String productId : productIds) {
            int quantity = 10 + random.nextInt(91); // Random quantity between 10 and 100

            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .productName("Product " + productId) // Placeholder, real name would come from Product Service
                    .availableQuantity(quantity)
                    .reservedQuantity(0)
                    .totalQuantity(quantity)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            inventoryRepository.save(inventory);
        }

        log.info("Seeding inventory completed. Created stock for {} products.", inventoryRepository.count());
    }
}
