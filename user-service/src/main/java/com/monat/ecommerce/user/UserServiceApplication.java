package com.monat.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service Main Application.
 * 
 * Educational Note:
 * - @EnableJpaAuditing: Enables automatic filling of fields like 'createdAt' 
 *   and 'updatedAt' in entities.
 */
@SpringBootApplication
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
