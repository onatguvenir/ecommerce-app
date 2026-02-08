package com.monat.ecommerce.order.domain.model;

/**
 * Saga orchestration steps
 */
public enum SagaStep {
    ORDER_CREATED,
    USER_VALIDATED,
    STOCK_RESERVED,
    PAYMENT_PROCESSED,
    ORDER_COMPLETED,
    COMPENSATION_STARTED,
    STOCK_RELEASED,
    PAYMENT_REFUNDED,
    COMPENSATION_COMPLETED
}
