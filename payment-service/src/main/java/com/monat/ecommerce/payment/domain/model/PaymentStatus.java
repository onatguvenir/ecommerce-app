package com.monat.ecommerce.payment.domain.model;

/**
 * Payment status enumeration
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
