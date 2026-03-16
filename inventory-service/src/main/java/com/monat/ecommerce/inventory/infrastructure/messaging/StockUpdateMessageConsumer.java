package com.monat.ecommerce.inventory.infrastructure.messaging;

import com.monat.ecommerce.inventory.domain.dto.StockUpdateMessage;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Spring Batch tarafından RabbitMQ'ya yazılan toplu stok güncellemelerini işler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockUpdateMessageConsumer {

    private final InventoryRepository inventoryRepository;

    @Transactional
    // RabbitMQConfig'te concurrency: 5, max-concurrency: 10 ayarlandığı için Spring otomatik limite uyar.
    @RabbitListener(queues = RabbitMQConfig.STOCK_UPDATE_QUEUE)
    public void handleStockUpdate(StockUpdateMessage message) {
        log.debug("Toplu stok güncelleme mesajı alındı - SKU: {}, Qty: {}, Opr: {}", 
                message.sku(), message.quantity(), message.operationType());

        // Pessimistic Write Lock uygulanarak DB I/O güvenliği sağlanır. (Bu örnekte repository'nin pessimistic
        // desteklediği varsayılır, aksi halde entity'de @Version field (Optimistic) vardır).
        // Inventory uygulaması zaten Optimistic Locking destekliyor (private Long version).
        
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
            log.info("Stok artırıldı - SKU: {}, Eklenen: {}, Yeni Toplam: {}", 
                    message.sku(), message.quantity(), inventory.getAvailableQuantity());
        } else if ("SET".equalsIgnoreCase(message.operationType())) {
            inventory.setStock(message.quantity());
            log.info("Stok eşitlendi - SKU: {}, Yeni Stok: {}", 
                    message.sku(), inventory.getAvailableQuantity());
        }

        inventoryRepository.save(inventory);
    }
}
