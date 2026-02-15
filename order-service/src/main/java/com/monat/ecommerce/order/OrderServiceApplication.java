package com.monat.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Service Application.
 * <p>
 * This service manages customer orders.
 * It exposes REST APIs for clients and uses gRPC/Feign for inter-service
 * communication.
 * </p>
 * 
 * @SpringBootApplication acts as the main configuration class.
 * 
 * @EnableFeignClients enables declarative REST clients (Feign) to call other
 *                     services (legacy/REST fallback).
 * 
 * @EnableJpaAuditing enables automatic population of auditing fields.
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
