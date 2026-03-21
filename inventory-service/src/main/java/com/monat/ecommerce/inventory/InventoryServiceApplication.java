package com.monat.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inventory Service Main Application.
 * 
 * Educational Note:
 * - @EnableCaching: Enables Redis-based caching to speed up common stock checks.
 * - @EnableScheduling: Used for stock-related house-keeping tasks (e.g., expiring old reservations).
 */
@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.inventory",
        "com.monat.ecommerce.common"
})
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
