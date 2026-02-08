package com.monat.ecommerce.inventory.application.service;

import com.monat.ecommerce.inventory.application.dto.StockCheckResponse;
import com.monat.ecommerce.inventory.application.dto.UpdateStockRequest;
import com.monat.ecommerce.inventory.domain.model.InventoryItem;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryApplicationService
 */
@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private InventoryApplicationService inventoryApplicationService;

    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        inventoryItem = InventoryItem.builder()
                .id(1L)
                .productId("PROD-001")
                .quantity(100)
                .reservedQuantity(0)
                .version(0L)
                .build();

        // Mock cache
        when(cacheManager.getCache("inventory"))
                .thenReturn(new ConcurrentMapCache("inventory"));
    }

    @Test
    void checkStock_Found() {
        // Given
        when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));

        // When
        StockCheckResponse response = inventoryApplicationService.checkStock("PROD-001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("PROD-001");
        assertThat(response.getQuantity()).isEqualTo(100);
        assertThat(response.getAvailableQuantity()).isEqualTo(100);
        verify(inventoryRepository, times(1)).findByProductId("PROD-001");
    }

    @Test
    void checkStock_NotFound() {
        // Given
        when(inventoryRepository.findByProductId("INVALID"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryApplicationService.checkStock("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory not found");

        verify(inventoryRepository, times(1)).findByProductId("INVALID");
    }

    @Test
    void updateStock_Success() {
        // Given
        when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        UpdateStockRequest request = UpdateStockRequest.builder()
                .productId("PROD-001")
                .quantity(150)
                .build();

        // When
        StockCheckResponse response = inventoryApplicationService.updateStock(request);

        // Then
        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void reserveStock_Success() {
        // Given
        when(inventoryRepository.findByProductIdWithLock("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        // When
        boolean result = inventoryApplicationService.reserveStock("PROD-001", 10, "ORDER-123");

        // Then
        assertThat(result).isTrue();
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void reserveStock_InsufficientStock() {
        // Given
        when(inventoryRepository.findByProductIdWithLock("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));

        // When
        boolean result = inventoryApplicationService.reserveStock("PROD-001", 200, "ORDER-123");

        // Then
        assertThat(result).isFalse();
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void releaseReservation_Success() {
        // Given
        inventoryItem.setReservedQuantity(10);
        when(inventoryRepository.findByProductIdWithLock("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        // When
        inventoryApplicationService.releaseReservation("PROD-001", 10, "ORDER-123");

        // Then
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void confirmReservation_Success() {
        // Given
        inventoryItem.setReservedQuantity(10);
        when(inventoryRepository.findByProductIdWithLock("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        // When
        inventoryApplicationService.confirmReservation("PROD-001", 10, "ORDER-123");

        // Then
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void createInventory_Success() {
        // Given
        when(inventoryRepository.existsByProductId("PROD-NEW")).thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        // When
        StockCheckResponse response = inventoryApplicationService.createInventory("PROD-NEW", 50);

        // Then
        assertThat(response).isNotNull();
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void createInventory_AlreadyExists() {
        // Given
        when(inventoryRepository.existsByProductId("PROD-001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> inventoryApplicationService.createInventory("PROD-001", 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void cacheEviction_OnUpdate() {
        // Given
        when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        UpdateStockRequest request = UpdateStockRequest.builder()
                .productId("PROD-001")
                .quantity(150)
                .build();

        // When
        inventoryApplicationService.updateStock(request);

        // Then
        verify(cacheManager, atLeastOnce()).getCache("inventory");
    }
}
