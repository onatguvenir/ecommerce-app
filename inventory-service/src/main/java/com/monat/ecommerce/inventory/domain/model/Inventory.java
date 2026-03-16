package com.monat.ecommerce.inventory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inventory aggregate root - Pure Domain Object
 * 
 * This implementation uses a version field to support optimistic locking
 * in the persistence layer, but the domain model itself is agnostic of JPA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    private UUID id;
    private String productId;
    private String productName;

    @Builder.Default
    private Integer availableQuantity = 0;

    @Builder.Default
    private Integer reservedQuantity = 0;

    @Builder.Default
    private Integer totalQuantity = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Optimistic lock version
     */
    private Long version;

    /**
     * Reserve stock for an order
     * 
     * @throws IllegalStateException if insufficient stock available
     */
    public void reserveStock(Integer quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            productId, availableQuantity, quantity));
        }

        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    /**
     * Release reserved stock (compensation)
     */
    public void releaseReservedStock(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot release more than reserved. Reserved: %d, Requested release: %d",
                            reservedQuantity, quantity));
        }

        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    /**
     * Commit reservation (finalize sale)
     */
    public void commitReservation(Integer quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Cannot commit more than reserved. Reserved: %d, Requested commit: %d",
                            reservedQuantity, quantity));
        }

        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    /**
     * Add stock (replenishment)
     */
    public void addStock(Integer quantity) {
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
    }

    /**
     * Overwrite stock to a specific available quantity
     */
    public void setStock(Integer quantity) {
        this.availableQuantity = quantity;
        this.totalQuantity = this.availableQuantity + this.reservedQuantity;
    }

    /**
     * Check if stock is available
     */
    public boolean isStockAvailable(Integer quantity) {
        return availableQuantity >= quantity;
    }
}
