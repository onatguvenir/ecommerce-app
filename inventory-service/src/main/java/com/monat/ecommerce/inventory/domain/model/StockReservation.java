package com.monat.ecommerce.inventory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock reservation domain model - Pure POJO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    private UUID id;
    private String reservationId;
    private String orderId;
    private String productId;
    private Integer quantity;

    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    /**
     * For optimistic locking
     */
    private Long version;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsCommitted() {
        this.status = ReservationStatus.COMMITTED;
    }

    public void markAsReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public void markAsExpired() {
        this.status = ReservationStatus.EXPIRED;
    }
}
