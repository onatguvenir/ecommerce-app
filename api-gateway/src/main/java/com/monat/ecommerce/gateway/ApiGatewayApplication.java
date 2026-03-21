package com.monat.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Entry Point.
 * 
 * Architecture Note:
 * The Gateway serves as the single entry point (BFF - Backend for Frontend pattern) 
 * for all external requests. It provides:
 * 1. Dynamic Routing: Forwards requests to microservices based on URL path.
 * 2. Cross-cutting Concerns: Authentication, Rate Limiting, and Tracing/Logging.
 * 3. Security: Shielding internal microservices from direct public access.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
