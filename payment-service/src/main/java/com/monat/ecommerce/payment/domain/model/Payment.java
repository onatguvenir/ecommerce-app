package com.monat.ecommerce.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment domain model - Pure POJO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private UUID id;
    private String idempotencyKey;
    private String orderId;
    private String userId;
    private String paymentReference;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    private PaymentMethod paymentMethod;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String failureReason;
    private String refundReference;
    private BigDecimal refundedAmount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long version;

    // Helper methods
    public void markAsSuccessful(String paymentReference) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentReference = paymentReference;
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsRefunded(String refundRef, BigDecimal refundAmount) {
        this.status = PaymentStatus.REFUNDED;
        this.refundReference = refundRef;
        this.refundedAmount = refundAmount;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean canBeRefunded() {
        return this.status == PaymentStatus.COMPLETED;
    }
}
