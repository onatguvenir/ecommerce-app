package com.monat.ecommerce.order.domain.model;

/**
 * Saga execution status
 */
public enum SagaStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
