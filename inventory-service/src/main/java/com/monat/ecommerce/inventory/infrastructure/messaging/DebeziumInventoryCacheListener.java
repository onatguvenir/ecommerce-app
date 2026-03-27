package com.monat.ecommerce.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.inventory.infrastructure.config.InventoryMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-Driven Cache Eviction via Debezium CDC.
 * 
 * Instead of evicting cache synchronously inside the business transaction (which is error-prone
 * and causes dual-write anomalies), this listener consumes WAL (Write-Ahead Log) events
 * published by Debezium to Kafka.
 * 
 * When a stock update is committed to PostgreSQL, it guarantees a Kafka message is produced.
 * We parse the JSON payload, find the `product_id`, and safely evict it from the Redis cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumInventoryCacheListener {

    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final InventoryMetrics inventoryMetrics;

    @KafkaListener(topics = "cdc.public.inventory", groupId = "inventory-cache-evictor-group")
    public void onInventoryChanged(String message) {
        try {
            log.debug("Received CDC event from Debezium: {}", message);
            JsonNode rootNode = objectMapper.readTree(message);
            
            // Due to schema-less configuration, payload roots may vary.
            JsonNode payloadNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;
            
            if (payloadNode == null || payloadNode.isNull()) return;

            // Extract the 'after' state. For deletes, 'after' is null, we use 'before'
            JsonNode stateNode = payloadNode.has("after") && !payloadNode.get("after").isNull() 
                    ? payloadNode.get("after") 
                    : payloadNode.get("before");

            if (stateNode != null && stateNode.has("product_id")) {
                String productId = stateNode.get("product_id").asText();
                evictCache(productId);
                inventoryMetrics.incrementCacheEviction("success");
            }
        } catch (Exception e) {
            log.error("Failed to process Debezium CDC message for cache eviction", e);
            inventoryMetrics.incrementCacheEviction("failure");
        }
    }

    private void evictCache(String productId) {
        Cache cache = cacheManager.getCache("inventory");
        if (cache != null && productId != null) {
            cache.evict(productId);
            log.info("Redis cache evicted successfully for inventory product_id: {}", productId);
        }
    }
}
