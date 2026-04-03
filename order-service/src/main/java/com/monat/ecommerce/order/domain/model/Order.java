package com.monat.ecommerce.order.domain.model;

import com.monat.ecommerce.common.model.AbstractOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Order aggregate root - Pure Domain Object
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends AbstractOrder<OrderItem> {

    private UUID id;
    private String orderNumber;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private ShippingAddress shippingAddress;

    private String paymentReference;
    private String cancellationReason;

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
