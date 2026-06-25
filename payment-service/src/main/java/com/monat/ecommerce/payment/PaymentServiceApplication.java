package com.monat.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service Main Application.
 *
 * Educational Note:
 * - @EnableRetry: Critical for financial services. Enables automatic retries
 *   for transient failures like deadlocks or connection issues.
 * - @EnableScheduling: Required for the Transactional Outbox Pattern — drives
 *   PaymentOutboxEventPublisher's @Scheduled poller that publishes persisted
 *   payment events to Kafka. Without it, outbox rows are never published.
 */
@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.payment",
        "com.monat.ecommerce.common"
})
@EnableJpaAuditing
@EnableRetry
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
