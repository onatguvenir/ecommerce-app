package com.monat.ecommerce.inventory.domain.service;

import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import com.monat.ecommerce.inventory.domain.repository.InventoryRepository;
import com.monat.ecommerce.inventory.domain.repository.StockReservationRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Inventory domain service with optimistic locking and retry mechanism
 * 
 * When optimistic lock conflicts occur (OptimisticLockException), the operation
 * is automatically retried with exponential backoff. This ensures high throughput
 * while preventing overselling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDomainService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    @Value("${application.reservation.expiry-minutes:15}")
    private Integer reservationExpiryMinutes;

    /**
     * Reserve stock for an order - with retry on optimistic lock failures
     * 
     * @Retryable automatically retries if OptimisticLockException occurs
     * This allows multiple concurrent requests to succeed sequentially
     */
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 2000)
    )
    @CacheEvict(value = "inventory", key = "#productId")
    public String reserveStock(String orderId, String productId, Integer quantity) {
        log.info("Reserving stock - Order: {}, Product: {}, Qty: {}", orderId, productId, quantity);

        // Find inventory
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Reserve stock (throws exception if insufficient)
        inventory.reserveStock(quantity);
        
        // Save with optimistic locking
        inventoryRepository.save(inventory);

        // Create reservation record
        String reservationId = UUID.randomUUID().toString();
        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(reservationExpiryMinutes))
                .build();

        reservationRepository.save(reservation);

        log.info("Stock reserved successfully - Reservation ID: {}, Version: {}", 
                reservationId, inventory.getVersion());

        return reservationId;
    }

    /**
     * Reserve multiple products atomically
     */
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 2000)
    )
    public String reserveMultipleProducts(String orderId, Map<String, Integer> productQuantities) {
        log.info("Reserving multiple products for order: {}", orderId);

        String reservationId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationExpiryMinutes);

        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            // Find and reserve
            Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            inventory.reserveStock(quantity);
            inventoryRepository.save(inventory);

            // Create reservation
            StockReservation reservation = StockReservation.builder()
                    .reservationId(reservationId)
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .status(ReservationStatus.ACTIVE)
                    .expiresAt(expiresAt)
                    .build();

            reservationRepository.save(reservation);
        }

        log.info("Multiple products reserved - Reservation ID: {}", reservationId);
        return reservationId;
    }

    /**
     * Release reserved stock (compensation)
     */
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 2000)
    )
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: {}", reservationId);

        List<StockReservation> reservations = reservationRepository.findByReservationId(reservationId)
                .stream().toList();

        if (reservations.isEmpty()) {
            log.warn("Reservation not found: {}", reservationId);
            return;
        }

        for (StockReservation reservation : reservations) {
            // Skip if already released or committed
            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                continue;
            }

            // Release stock
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found: " + reservation.getProductId()));

            inventory.releaseReservedStock(reservation.getQuantity());
            inventoryRepository.save(inventory);

            // Mark reservation as released
            reservation.markAsReleased();
            reservationRepository.save(reservation);

            log.info("Stock released - Product: {}, Qty: {}", reservation.getProductId(), reservation.getQuantity());
        }
    }

    /**
     * Commit reservation (finalize sale)
     */
    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 2000)
    )
    public void commitReservation(String reservationId) {
        log.info("Committing reservation: {}", reservationId);

        List<StockReservation> reservations = reservationRepository.findByReservationId(reservationId)
                .stream().toList();

        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                log.warn("Reservation already processed: {}", reservation.getId());
                continue;
            }

            // Commit stock
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found: " + reservation.getProductId()));

            inventory.commitReservation(reservation.getQuantity());
            inventoryRepository.save(inventory);

            // Mark reservation as committed
            reservation.markAsCommitted();
            reservationRepository.save(reservation);

            log.info("Stock committed - Product: {}, Qty: {}", reservation.getProductId(), reservation.getQuantity());
        }
    }

    /**
     * Check stock availability with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory", key = "#productId")
    public boolean checkAvailability(String productId, Integer quantity) {
        return inventoryRepository.hasAvailableStock(productId, quantity);
    }

    /**
     * Get inventory by product ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory", key = "#productId")
    public Inventory getInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
