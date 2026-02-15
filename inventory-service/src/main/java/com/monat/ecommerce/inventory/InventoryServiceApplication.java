package com.monat.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Inventory Service Application.
 * <p>
 * This service manages product inventory levels.
 * It primarily communicates via gRPC for high performance.
 * </p>
 * 
 * @SpringBootApplication acts as the main configuration class.
 * 
 * @EnableJpaAuditing enables automatic population of auditing fields
 *                    (created_at, updated_at).
 * 
 * @EnableCaching enables Spring's caching infrastructure (e.g., using Redis or
 *                Caffeine).
 * 
 * @EnableScheduling enables Spring's scheduled task execution (e.g., for
 *                   background jobs).
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
