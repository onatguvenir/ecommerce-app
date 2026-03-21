package com.monat.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiter configuration using Redis
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver for rate limiting.
     * 
     * Educational Note:
     * This defines the 'tenant' for rate limits. By using the Remote Address (IP), 
     * we ensure that one client cannot overwhelm the system. 
     * In production, this might be a 'User ID' from a JWT to provide per-user limits.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress()
        );
    }
}
