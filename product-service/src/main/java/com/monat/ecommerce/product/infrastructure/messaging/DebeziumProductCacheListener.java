package com.monat.ecommerce.product.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-Driven Cache Eviction via Debezium CDC for MongoDB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumProductCacheListener {

    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @KafkaListener(topics = "cdc.productdb.products", groupId = "product-cache-evictor-group")
    public void onProductChanged(String message) {
        try {
            log.debug("Received MongoDB CDC event from Debezium: {}", message);
            JsonNode rootNode = objectMapper.readTree(message);
            
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;
            if (payloadNode == null || payloadNode.isNull()) return;

            JsonNode afterNode = payloadNode.get("after");
            // MongoDB CDC payload parses differently since 'after' is often a JSON string block of the document (JSON format)
            if (afterNode != null && afterNode.isTextual()) {
                JsonNode document = objectMapper.readTree(afterNode.asText());
                if (document.has("productId")) {
                    evictCache(document.get("productId").asText());
                }
            } else if (afterNode != null && afterNode.isObject()) {
                 if (afterNode.has("productId")) {
                     evictCache(afterNode.get("productId").asText());
                 }
            }
        } catch (Exception e) {
            log.error("Failed to process MongoDB CDC message for product cache eviction", e);
        }
    }

    private void evictCache(String productId) {
        Cache cache = cacheManager.getCache("products");
        if (cache != null && productId != null) {
            cache.evict(productId);
            log.info("Redis cache evicted successfully for product_id: {}", productId);
        }
    }
}
