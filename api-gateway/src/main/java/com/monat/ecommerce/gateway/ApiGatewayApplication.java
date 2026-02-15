package com.monat.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application.
 * <p>
 * This is the entry point for all external traffic.
 * It routes requests to appropriate microservices (User, Product, Order, etc.).
 * It handles cross-cutting concerns like authentication, rate limiting, and
 * logging.
 * </p>
 * 
 * @SpringBootApplication acts as the main configuration class.
 *                        Spring Cloud Gateway is likely configured via
 *                        application.yml.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
