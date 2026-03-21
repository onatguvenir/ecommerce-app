package com.monat.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Service Main Application.
 * 
 * Educational Note:
 * - @EnableScheduling: Required for the Transactional Outbox Pattern. 
 *   It enables the scheduled poller that sends events from the DB to Kafka.
 * - @EnableFeignClients: Allows declarative REST communication with other services.
 */
@SpringBootApplication(scanBasePackages = {
        "com.monat.ecommerce.order",
        "com.monat.ecommerce.common"
})
@EnableJpaAuditing
@EnableScheduling
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
