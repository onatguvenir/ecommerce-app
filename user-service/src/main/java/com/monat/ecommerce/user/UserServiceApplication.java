package com.monat.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the User Service Application.
 *
 * @SpringBootApplication is a convenience annotation that adds all of the
 *                        following:
 *                        - @Configuration: Tags the class as a source of bean
 *                        definitions for the application context.
 *                        - @EnableAutoConfiguration: Tells Spring Boot to start
 *                        adding beans based on classpath settings, other beans,
 *                        and various property settings.
 *                        - @ComponentScan: Tells Spring to look for other
 *                        components, configurations, and services in the
 *                        com/monat/ecommerce package.
 */
@SpringBootApplication
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
