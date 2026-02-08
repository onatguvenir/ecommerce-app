package com.monat.ecommerce.inventory.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock reservation entity - tracks temporary reservations
 */
@Entity
@Table(name = "stock_reservations", indexes = {
        @Index(name = "idx_reservation_id", columnList = "reservation_id", unique = true),
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_id", unique = true, nullable = false)
    private String reservationId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
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
