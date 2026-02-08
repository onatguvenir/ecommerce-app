package com.monat.ecommerce.inventory.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inventory entity with OPTIMISTIC LOCKING for concurrent stock updates
 * 
 * This implementation uses @Version to handle concurrent stock modifications efficiently.
 * When multiple requests try to update the same inventory item, only one succeeds per version,
 * preventing overselling while maintaining high throughput.
 */
@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Optimistic lock version - automatically incremented on each update
     * Prevents lost updates in concurrent scenarios
     */
    @Version
    private Long version;

    /**
     * Reserve stock for an order
     * @throws IllegalStateException if insufficient stock available
     */
    public void reserveStock(Integer quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                            productId, availableQuantity, quantity)
            );
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
                            reservedQuantity, quantity)
            );
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
                            reservedQuantity, quantity)
            );
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
     * Check if stock is available
     */
    public boolean isStockAvailable(Integer quantity) {
        return availableQuantity >= quantity;
    }
}
