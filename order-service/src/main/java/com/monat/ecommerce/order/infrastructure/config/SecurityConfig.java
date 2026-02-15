package com.monat.ecommerce.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Order Service
 * Allows public access to Swagger UI, API docs, and actuator endpoints
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to Swagger UI and API docs
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**")
                        .permitAll()
                        // Allow public access to actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // Allow public access to all order endpoints (for development)
                        .requestMatchers("/api/orders/**").permitAll()
                        // Allow all other requests (can be restricted later)
                        .anyRequest().permitAll());

        return http.build();
    }
}
