package com.monat.ecommerce.product.integration;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.query.GetProductQuery;
import com.monat.ecommerce.product.application.query.handler.GetProductQueryHandler;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Product Cache Integration Test.
 * 
 * Educational Note:
 * This test verifies that the Spring Cache abstraction successfully 
 * interacts with Redis. It ensures that subsequent calls for the same 
 * product are served from the cache, reducing database load.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Product Cache Verification Tests")
class ProductCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
        
        // Disable other infrastructure to speed up context load
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/unused");
        registry.add("spring.elasticsearch.uris", () -> "http://localhost:9200");
    }

    @Autowired
    private GetProductQueryHandler queryHandler;

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }
    }

    @Test
    @DisplayName("Should cache product response and avoid subsequent repository calls (Cache Hit)")
    void shouldCacheProductResponse() {
        // Arrange
        String productId = "PROD-CACHE-001";
        Product product = Product.builder()
                .productId(productId)
                .name("Cached Product")
                .price(BigDecimal.valueOf(100.0))
                .build();
        
        when(productRepository.findByProductId(productId)).thenReturn(Optional.of(product));
        
        GetProductQuery query = new GetProductQuery(productId);

        // Act
        ProductResponse firstCall = queryHandler.handle(query);
        ProductResponse secondCall = queryHandler.handle(query);

        // Assert
        assertThat(firstCall).isNotNull();
        assertThat(secondCall).isNotNull();
        assertThat(firstCall.productId()).isEqualTo(secondCall.productId());
        
        // Verify repository was called ONLY ONCE, second call was a cache hit
        verify(productRepository, times(1)).findByProductId(productId);
    }
}
