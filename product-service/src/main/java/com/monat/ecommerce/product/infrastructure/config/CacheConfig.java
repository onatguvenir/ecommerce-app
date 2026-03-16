package com.monat.ecommerce.product.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Konfigürasyonu.
 * <p>
 * Cache Stratejisi:
 * - Serialization: JSON (GenericJackson2JsonRedisSerializer) — human-readable,
 * debug kolaylığı
 * - Key: String (StringRedisSerializer) — okunabilir Redis key'leri
 * - TTL: "products" cache için 10 dakika
 * <p>
 * Neden JSON serialization?
 * - Java serialization'a göre daha portable ve debug edilebilir.
 * - Redis CLI ile doğrudan okunabilir.
 * - Farklı JVM versiyonları arasında uyumluluk sorunu yaşanmaz.
 * <p>
 * 
 * @EnableCaching: Spring'in cache proxy'lerini aktif eder.
 *                 Bu olmadan @Cacheable, @CacheEvict anotasyonları çalışmaz.
 *                 </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * RedisCacheManager: Spring Cache abstraction'ı Redis üzerine bağlar.
     * Tüm @Cacheable anotasyonları bu manager üzerinden Redis'e erişir.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON serializer — type bilgisini de saklar (deserialization için gerekli)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Polymorphic type handling: Redis'ten deserialize ederken doğru sınıfı bulmak
        // için
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache konfigürasyonu
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Key prefix: cache adı + "::" + key (örn: "products::PROD-001")
                .computePrefixWith(cacheName -> cacheName + "::")
                // Key serializer: String
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Value serializer: JSON
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                // Null değerleri cache'leme (ResourceNotFoundException'ı önler)
                .disableCachingNullValues()
                // Default TTL: 10 dakika
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // "products" cache için özel TTL: 10 dakika
                .withCacheConfiguration("products",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .build();
    }
}
