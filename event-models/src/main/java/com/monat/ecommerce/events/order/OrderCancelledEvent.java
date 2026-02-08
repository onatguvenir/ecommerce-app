package com.monat.ecommerce.events.order;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when order is cancelled
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent extends BaseEvent {
    private String orderId;
    private String orderNumber;
    private String userId;
    private String userEmail;
    private String reason;
    private String cancelledBy;
}
