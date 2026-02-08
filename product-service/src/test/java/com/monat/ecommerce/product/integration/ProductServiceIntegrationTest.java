package com.monat.ecommerce.product.integration;

import com.monat.ecommerce.product.ProductServiceApplication;
import com.monat.ecommerce.product.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Product Service with MongoDB and Elasticsearch
 */
@SpringBootTest(classes = ProductServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class ProductServiceIntegrationTest {

        @Container
        static MongoDBContainer mongodb = new MongoDBContainer(
                        DockerImageName.parse("mongo:7"))
                        .withExposedPorts(27017);

        @Container
        static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
                        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0"))
                        .withEnv("xpack.security.enabled", "false")
                        .withEnv("discovery.type", "single-node");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
                registry.add("spring.elasticsearch.uris", () -> "http://" +
                                elasticsearch.getHttpHostAddress());
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        void setUp() {
                productRepository.deleteAll();
        }

        @Test
        void createProduct_Success() throws Exception {
                // Given
                String productJson = """
                                {
                                  "productId": "PROD-MBP-001",
                                  "name": "MacBook Pro 16-inch",
                                  "description": "Powerful laptop for professionals",
                                  "price": 2499.99,
                                  "category": "Electronics",
                                  "brand": "Apple"
                                }
                                """;

                // When & Then
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productJson))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.name", is("MacBook Pro 16-inch")))
                                .andExpect(jsonPath("$.data.price", is(2499.99)))
                                .andExpect(jsonPath("$.data.category", is("Electronics")));
        }

        @Test
        void searchProducts_WithElasticsearch() throws Exception {
                // Given - Create products first
                createTestProduct("MacBook Pro", "Laptop", 2499.99);
                createTestProduct("MacBook Air", "Laptop", 1299.99);
                createTestProduct("iPhone 15", "Smartphone", 999.99);

                Thread.sleep(2000); // Wait for Elasticsearch indexing

                // When & Then - Search for "macbook"
                mockMvc.perform(get("/api/products/search?keyword=macbook"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        void searchProducts_WithPriceRange() throws Exception {
                // Given
                createTestProduct("Budget Phone", "Smartphone", 299.99);
                createTestProduct("Premium Phone", "Smartphone", 1299.99);

                Thread.sleep(2000);

                // When & Then - Search with price filter
                mockMvc.perform(get("/api/products/search")
                                .param("minPrice", "1000")
                                .param("maxPrice", "1500"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content[0].price",
                                                greaterThanOrEqualTo(1000.0)));
        }

        @Test
        void getProduct_Success() throws Exception {
                // Given
                String productId = createTestProduct("Test Product", "Test", 99.99);

                // When & Then
                mockMvc.perform(get("/api/products/" + productId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.productId", is(productId)))
                                .andExpect(jsonPath("$.data.name", is("Test Product")));
        }

        @Test
        void updateProduct_Success() throws Exception {
                // Given
                String productId = createTestProduct("Old Name", "Test", 99.99);

                String updateJson = String.format("""
                                {
                                  "productId": "%s",
                                  "name": "New Name",
                                  "description": "Updated description",
                                  "price": 149.99,
                                  "category": "Test",
                                  "brand": "TestBrand"
                                }
                                """, productId);

                // When & Then
                mockMvc.perform(put("/api/products/" + productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name", is("New Name")))
                                .andExpect(jsonPath("$.data.price", is(149.99)));
        }

        @Test
        void deleteProduct_Success() throws Exception {
                // Given
                String productId = createTestProduct("To Delete", "Test", 99.99);

                // When & Then
                mockMvc.perform(delete("/api/products/" + productId))
                                .andExpect(status().isOk());

                // Verify deleted
                mockMvc.perform(get("/api/products/" + productId))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getProductsByCategory_Success() throws Exception {
                // Given
                createTestProduct("Laptop 1", "Electronics", 999.99);
                createTestProduct("Laptop 2", "Electronics", 1299.99);
                createTestProduct("Book", "Books", 19.99);

                // When & Then
                mockMvc.perform(get("/api/products/category/Electronics"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                                .andExpect(jsonPath("$.data.content[0].category", is("Electronics")));
        }

        private String createTestProduct(String name, String category, double price) throws Exception {
                String productId = "PROD-TEST-" + System.currentTimeMillis();
                String productJson = String.format("""
                                {
                                  "productId": "%s",
                                  "name": "%s",
                                  "description": "Test description",
                                  "price": %.2f,
                                  "category": "%s",
                                  "brand": "TestBrand"
                                }
                                """, productId, name, price, category);

                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productJson))
                                .andExpect(status().isCreated());

                return productId;
        }
}
