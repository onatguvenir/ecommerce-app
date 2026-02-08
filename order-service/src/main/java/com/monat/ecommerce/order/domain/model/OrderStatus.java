package com.monat.ecommerce.order.domain.model;

/**
 * Order status enumeration
 */
public enum OrderStatus {
    PENDING,        // Order created, awaiting processing
    CONFIRMED,      // Stock reserved and payment processed
    COMPLETED,      // Order fulfilled
    CANCELLED,      // Order cancelled by user or system
    FAILED          // Order processing failed
}
