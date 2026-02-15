package com.monat.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Payment Service Application.
 * <p>
 * This service handles payment processing.
 * It likely communicates via gRPC for secure and fast transactions.
 * </p>
 * 
 * @SpringBootApplication acts as the main configuration class.
 * 
 * @EnableJpaAuditing enables automatic population of auditing fields
 *                    (created_at, updated_at).
 */
@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.payment",
        "com.monat.ecommerce.common"
})
@EnableJpaAuditing
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
