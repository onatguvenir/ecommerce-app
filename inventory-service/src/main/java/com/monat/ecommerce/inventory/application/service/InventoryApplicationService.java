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
import com.monat.ecommerce.inventory.infrastructure.config.InventoryMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Coordinator for Inventory-related business logic.
 * 
 * Educational Note:
 * This service manages 'Soft Reservations'. 
 * - reserveStock: Decrements 'available' and increments 'reserved' temporarily.
 * - confirmReservation: Finalizes the reservation, properly decrementing 'total'.
 * - cancelReservation: Releases 'reserved' back to 'available'.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryMetrics inventoryMetrics;

    @Transactional(readOnly = true)
    @Observed(name = "inventory.lookup", contextualName = "inventory-check-stock")
    public boolean checkStock(String productId, Integer quantity) {
        return inventoryRepository.hasAvailableStock(productId, quantity);
    }

    @Transactional
    public StockReservation reserveStock(StockReservationRequest request) {
        Timer.Sample sample = Timer.start();
        try {
            Inventory inventory = inventoryRepository.findByProductId(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

            inventory.reserveStock(request.quantity());
            inventoryRepository.save(inventory);

            StockReservation reservation = inventoryMapper.toReservation(request);
            reservation.setReservationId(UUID.randomUUID().toString());
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            reservation.setStatus(ReservationStatus.ACTIVE);

            StockReservation savedReservation = stockReservationRepository.save(reservation);
            inventoryMetrics.recordReservationQuantity(request.quantity(), "reserve", "success");
            return savedReservation;
        } catch (RuntimeException ex) {
            inventoryMetrics.recordReservationQuantity(request.quantity(), "reserve", "failure");
            throw ex;
        } finally {
            sample.stop(inventoryMetrics.reservationTimer());
        }
    }

    @Transactional
    public void confirmReservation(String reservationId) {
        Timer.Sample sample = Timer.start();
        try {
            StockReservation reservation = stockReservationRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

            inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reservation.getProductId()));

            if (reservation.isExpired()) {
                throw new IllegalStateException("Reservation expired: " + reservationId);
            }

            reservation.markAsCommitted();
            stockReservationRepository.save(reservation);
            inventoryMetrics.recordReservationQuantity(reservation.getQuantity(), "confirm", "success");
        } catch (RuntimeException ex) {
            inventoryMetrics.recordReservationQuantity(0, "confirm", "failure");
            throw ex;
        } finally {
            sample.stop(inventoryMetrics.reservationTimer());
        }
    }

    @Transactional
    public void cancelReservation(String reservationId) {
        Timer.Sample sample = Timer.start();
        try {
            StockReservation reservation = stockReservationRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

            reservation.markAsReleased();
            stockReservationRepository.save(reservation);
            inventoryMetrics.recordReservationQuantity(reservation.getQuantity(), "cancel", "success");
        } catch (RuntimeException ex) {
            inventoryMetrics.recordReservationQuantity(0, "cancel", "failure");
            throw ex;
        } finally {
            sample.stop(inventoryMetrics.reservationTimer());
        }
    }

    @Transactional
    public InventoryResponse addStock(String productId, Integer quantity) {
        Timer.Sample sample = Timer.start();
        try {
            Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElse(Inventory.builder()
                            .id(UUID.randomUUID())
                            .productId(productId)
                            .totalQuantity(0)
                            .availableQuantity(0)
                            .reservedQuantity(0)
                            .build());

            inventory.addStock(quantity);
            Inventory saved = inventoryRepository.save(inventory);
            inventoryMetrics.recordStockAdjustment(quantity, "add_stock");
            return inventoryMapper.toResponse(saved);
        } finally {
            sample.stop(inventoryMetrics.adjustmentTimer());
        }
    }

    @Transactional(readOnly = true)
    @Observed(name = "inventory.lookup", contextualName = "inventory-get-by-product-id")
    public InventoryResponse getInventory(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        return inventoryMapper.toResponse(inventory);
    }
}
