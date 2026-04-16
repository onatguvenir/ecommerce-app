package com.monat.ecommerce.product.integration;

import com.monat.ecommerce.product.application.dto.ProductResponse;
import com.monat.ecommerce.product.application.query.GetProductQuery;
import com.monat.ecommerce.product.application.query.handler.GetProductQueryHandler;
import com.monat.ecommerce.product.domain.model.Product;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import com.monat.ecommerce.product.domain.service.ProductSyncService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Product Cache Integration Test.
 *
 * <p>Verifies that the Spring Cache abstraction correctly interacts with Redis:
 * subsequent calls for the same product key must be served from cache (cache hit),
 * avoiding redundant repository calls.
 *
 * <p>Skip Strategy:
 * {@code @ExtendWith(DockerRequiredExtension.class)} is evaluated as a JUnit 5
 * ExecutionCondition BEFORE Spring context loading. If Docker is unavailable,
 * the test is SKIPPED — no Redis container is started.
 */
@ExtendWith(DockerRequiredExtension.class)
@SpringBootTest
@DisplayName("Product Cache Verification Tests")
class ProductCacheIntegrationTest {

    @SuppressWarnings("rawtypes")
    private static GenericContainer<?> redis;
    private static MongoDBContainer mongoDBContainer;
    private static ElasticsearchContainer elasticsearchContainer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // DockerRequiredExtension confirmed Docker is up; safe to start container.
        if (redis == null) {
            redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            redis.start();
        }

        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));
            mongoDBContainer.start();
        }
        if (elasticsearchContainer == null) {
            elasticsearchContainer = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.10.2"))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");
            elasticsearchContainer.start();
        }

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("management.tracing.enabled", () -> "false");
        registry.add("server.port", () -> "0");
    }

    @AfterAll
    static void stopContainers() {
        if (redis != null && redis.isRunning()) {
            redis.stop();
            redis = null;
        }
        if (mongoDBContainer != null && mongoDBContainer.isRunning()) {
            mongoDBContainer.stop();
            mongoDBContainer = null;
        }
        if (elasticsearchContainer != null && elasticsearchContainer.isRunning()) {
            elasticsearchContainer.stop();
            elasticsearchContainer = null;
        }
    }

    @Autowired
    private GetProductQueryHandler queryHandler;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }
        productRepository.deleteAll();
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
        productRepository.save(product);

        GetProductQuery query = new GetProductQuery(productId);

        // Act & Assert
        // Ilk cagrida veritabanindan cekilip cache'e koyulacak
        ProductResponse firstCall = queryHandler.handle(query);
        assertThat(firstCall).isNotNull();

        // Kanitlamak icin DB'den urunu siliyoruz! Eger 2. cagri bunu hala bulabiliyorsa %100 cache'den geliyordur.
        productRepository.deleteAll();

        // Ikinci cagri (Cache Hit)
        ProductResponse secondCall = queryHandler.handle(query);

        // Assert: both responses are identical, meaning it returned the cached object even though db is empty
        assertThat(secondCall).isNotNull();
        assertThat(firstCall.productId()).isEqualTo(secondCall.productId());
    }
}
