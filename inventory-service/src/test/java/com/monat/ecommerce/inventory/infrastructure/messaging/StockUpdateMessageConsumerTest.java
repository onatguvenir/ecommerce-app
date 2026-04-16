package com.monat.ecommerce.inventory.infrastructure.messaging;

import com.monat.ecommerce.inventory.domain.dto.StockUpdateMessage;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.infrastructure.config.InventoryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockUpdateMessageConsumerTest {

    @InjectMocks
    private StockUpdateMessageConsumer consumer;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMetrics inventoryMetrics;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId("SKU-100")
                .availableQuantity(50)
                .reservedQuantity(10)
                .totalQuantity(60)
                .build();
    }

    @Test
    void handleStockUpdate_existingInventory_addStock() {
        StockUpdateMessage message = new StockUpdateMessage("SKU-100", 25, "ADD", "ref-1");

        when(inventoryRepository.findByProductId("SKU-100")).thenReturn(Optional.of(inventory));

        consumer.handleStockUpdate(message);

        assertEquals(75, inventory.getAvailableQuantity());
        assertEquals(85, inventory.getTotalQuantity());
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void handleStockUpdate_existingInventory_setStock() {
        StockUpdateMessage message = new StockUpdateMessage("SKU-100", 200, "SET", "ref-2");

        when(inventoryRepository.findByProductId("SKU-100")).thenReturn(Optional.of(inventory));

        consumer.handleStockUpdate(message);

        // reserved was 10. if set sets available, total = available + reserved = 210
        assertEquals(200, inventory.getAvailableQuantity());
        assertEquals(210, inventory.getTotalQuantity());
        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void handleStockUpdate_newInventory_addStock() {
        StockUpdateMessage message = new StockUpdateMessage("SKU-200", 50, "ADD", null);

        when(inventoryRepository.findByProductId("SKU-200")).thenReturn(Optional.empty());

        consumer.handleStockUpdate(message);

        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }
}
