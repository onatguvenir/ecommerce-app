package com.monat.ecommerce.events.payment;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when payment fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent extends BaseEvent {
    private String paymentId;
    private String paymentReference;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private String errorCode;
}
