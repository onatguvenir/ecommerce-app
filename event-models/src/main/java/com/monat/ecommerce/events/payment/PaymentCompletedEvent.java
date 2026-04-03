package com.monat.ecommerce.events.payment;

import com.monat.ecommerce.events.BaseEvent;

import lombok.*;

import java.math.BigDecimal;

/**
 * Event published when payment is completed successfully
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent extends BaseEvent {
    private String paymentId;
    private String paymentReference;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
}
