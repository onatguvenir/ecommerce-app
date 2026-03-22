package com.monat.ecommerce.inventory.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.inventory.application.dto.InventoryMapper;
import com.monat.ecommerce.inventory.application.dto.InventoryResponse;
import com.monat.ecommerce.inventory.application.dto.StockReservationRequest;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.domain.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    // InventoryMapper, MapStruct aracılığıyla üretilen bir Spring bean'idir.
    // @InjectMocks'un doğru çalışması için mock'lanması gerekir.
    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryApplicationService inventoryApplicationService;

    private Inventory inventory;
    private StockReservationRequest reservationRequest;
    private UUID inventoryId;

    @BeforeEach
    void setUp() {
        inventoryId = UUID.randomUUID();
        inventory = Inventory.builder()
                .id(inventoryId)
                .productId("PROD-123")
                .totalQuantity(100)
                .availableQuantity(100)
                .reservedQuantity(0)
                .build();

        reservationRequest = StockReservationRequest.builder()
                .productId("PROD-123")
                .quantity(10)
                .orderId("ORDER-123")
                .build();
    }

    @Test
    void checkStock_Available() {
        when(inventoryRepository.hasAvailableStock("PROD-123", 10)).thenReturn(true);

        boolean result = inventoryApplicationService.checkStock("PROD-123", 10);

        assertThat(result).isTrue();
        verify(inventoryRepository).hasAvailableStock("PROD-123", 10);
    }

    @Test
    void reserveStock_Success() {
        when(inventoryRepository.findByProductId("PROD-123")).thenReturn(Optional.of(inventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> {
            StockReservation r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        StockReservation result = inventoryApplicationService.reserveStock(reservationRequest);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("PROD-123");
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getOrderId()).isEqualTo("ORDER-123");
        verify(inventoryRepository).findByProductId("PROD-123");
        verify(stockReservationRepository).save(any(StockReservation.class));
    }

    @Test
    void reserveStock_InsufficientStock() {
        // Mock inventory logic if necessary, here we rely on inventory object state
        // But wait, inventory.isStockAvailable(quantity) uses internal logic.
        // 100 > 10, so it should be fine.
        // Let's force insufficient stock scenario by mocking repository to return low
        // stock inventory
        Inventory lowStockInventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId("PROD-123")
                .totalQuantity(5)
                .availableQuantity(5)
                .reservedQuantity(0)
                .build();

        when(inventoryRepository.findByProductId("PROD-123")).thenReturn(Optional.of(lowStockInventory));

        assertThatThrownBy(() -> inventoryApplicationService.reserveStock(reservationRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void confirmReservation_Success() {
        String reservationId = "RES-123";
        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .productId("PROD-123")
                .quantity(10)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(stockReservationRepository.findByReservationId(reservationId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductId("PROD-123")).thenReturn(Optional.of(inventory));

        inventoryApplicationService.confirmReservation(reservationId);

        verify(stockReservationRepository).save(reservation);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void addStock_NewProduct() {
        when(inventoryRepository.findByProductId("PROD-456")).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory i = invocation.getArgument(0);
            i.setProductId("PROD-456");
            return i;
        });

        InventoryResponse response = inventoryApplicationService.addStock("PROD-456", 50);

        // InventoryResponse bir Java Record olduğundan getter değil, accessor metod syntax'ı kullanılır.
        assertThat(response.productId()).isEqualTo("PROD-456");
        assertThat(response.quantity()).isEqualTo(50);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void getInventory_Success() {
        when(inventoryRepository.findByProductId("PROD-123")).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryApplicationService.getInventory("PROD-123");

        // InventoryResponse bir Java Record olduğundan getter değil, accessor metod syntax'ı kullanılır.
        assertThat(response.productId()).isEqualTo("PROD-123");
        assertThat(response.quantity()).isEqualTo(100);
    }
}
