package com.monat.ecommerce.inventory.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.annotation.EnableRetry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableRetry
public class CacheConfig {

    // Cache names
    static final String INVENTORY_CACHE = "inventory";
    static final String RESERVATIONS_CACHE = "reservations";

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(INVENTORY_CACHE, RESERVATIONS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2000));
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager) {
        // L1: Caffeine (local, 5 min) → L2: Redis (distributed, 60 min)
        CompositeCacheManager composite = new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
        composite.setFallbackToNoOpCache(false);
        return composite;
    }
}
