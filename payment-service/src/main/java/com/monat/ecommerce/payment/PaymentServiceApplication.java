package com.monat.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Payment Service Main Application.
 * 
 * Educational Note:
 * - @EnableRetry: Critical for financial services. Enables automatic retries 
 *   for transient failures like deadlocks or connection issues.
 */
@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.payment",
        "com.monat.ecommerce.common"
})
@EnableJpaAuditing
@EnableRetry
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
