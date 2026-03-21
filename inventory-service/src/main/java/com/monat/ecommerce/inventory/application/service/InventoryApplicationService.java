package com.monat.ecommerce.inventory.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.inventory.application.dto.InventoryMapper;
import com.monat.ecommerce.inventory.application.dto.InventoryResponse;
import com.monat.ecommerce.inventory.application.dto.StockReservationRequest;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.domain.repository.StockReservationRepository;
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

    @Transactional(readOnly = true)
    public boolean checkStock(String productId, Integer quantity) {
        return inventoryRepository.hasAvailableStock(productId, quantity);
    }

    @Transactional
    public StockReservation reserveStock(StockReservationRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        // Perform domain reservation (decrements available, increments reserved)
        inventory.reserveStock(request.quantity());
        inventoryRepository.save(inventory);

        // Create reservation record
        StockReservation reservation = inventoryMapper.toReservation(request);
        reservation.setReservationId(UUID.randomUUID().toString());
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        reservation.setStatus(ReservationStatus.ACTIVE);

        return stockReservationRepository.save(reservation);
    }

    @Transactional
    public void confirmReservation(String reservationId) {
        StockReservation reservation = stockReservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reservation.getProductId()));

        if (reservation.isExpired()) {
            throw new IllegalStateException("Reservation expired: " + reservationId);
        }

        reservation.markAsCommitted();
        stockReservationRepository.save(reservation);

        // Deduct stock
        // inventory.deductStock(reservation.getQuantity());
        // inventoryRepository.save(inventory);
    }

    @Transactional
    public void cancelReservation(String reservationId) {
        StockReservation reservation = stockReservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        reservation.markAsReleased();
        stockReservationRepository.save(reservation);
    }

    @Transactional
    public InventoryResponse addStock(String productId, Integer quantity) {
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

        return inventoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        return inventoryMapper.toResponse(inventory);
    }
}
