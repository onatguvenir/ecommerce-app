package com.monat.ecommerce.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate root - Pure Domain Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private UUID id;
    private String orderNumber;
    private UUID userId;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private BigDecimal totalAmount;

    @Builder.Default
    private String currency = "USD";

    private ShippingAddress shippingAddress;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private String paymentReference;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For Optimistic Locking
    private Long version;

    // Helper methods
    public void addItem(OrderItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    public void removeItem(OrderItem item) {
        if (items != null) {
            items.remove(item);
        }
    }

    public void markAsPending() {
        this.status = OrderStatus.PENDING;
    }

    public void markAsConfirmed() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markAsCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.cancellationReason = reason;
    }
}
