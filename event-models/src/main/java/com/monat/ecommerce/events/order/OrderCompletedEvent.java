package com.monat.ecommerce.events.order;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when order is completed successfully
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent extends BaseEvent {
    private String orderId;
    private String orderNumber;
    private String userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentReference;
}
