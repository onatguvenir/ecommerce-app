package com.monat.ecommerce.inventory.infrastructure.messaging;

import com.monat.ecommerce.inventory.domain.dto.StockUpdateMessage;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.infrastructure.config.InventoryMetrics;
import com.monat.ecommerce.inventory.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumes bulk stock update messages produced through RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockUpdateMessageConsumer {

    private final InventoryRepository inventoryRepository;
    private final InventoryMetrics inventoryMetrics;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.STOCK_UPDATE_QUEUE)
    public void handleStockUpdate(StockUpdateMessage message) {
        log.debug("Received bulk stock update - SKU: {}, Qty: {}, Operation: {}",
                message.sku(), message.quantity(), message.operationType());

        Inventory inventory = inventoryRepository.findByProductId(message.sku())
                .orElse(Inventory.builder()
                        .id(UUID.randomUUID())
                        .productId(message.sku())
                        .totalQuantity(0)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build());

        if ("ADD".equalsIgnoreCase(message.operationType())) {
            inventory.addStock(message.quantity());
            inventoryMetrics.incrementBulkUpdate("ADD", "success");
            inventoryMetrics.recordStockAdjustment(message.quantity(), "bulk_add");
            log.info("Stock increased - SKU: {}, Added: {}, New available: {}",
                    message.sku(), message.quantity(), inventory.getAvailableQuantity());
        } else if ("SET".equalsIgnoreCase(message.operationType())) {
            inventory.setStock(message.quantity());
            inventoryMetrics.incrementBulkUpdate("SET", "success");
            inventoryMetrics.recordStockAdjustment(message.quantity(), "bulk_set");
            log.info("Stock set - SKU: {}, New available: {}",
                    message.sku(), inventory.getAvailableQuantity());
        } else {
            inventoryMetrics.incrementBulkUpdate(message.operationType(), "ignored");
            log.warn("Unsupported stock update operation: {}", message.operationType());
            return;
        }

        inventoryRepository.save(inventory);
    }
}
