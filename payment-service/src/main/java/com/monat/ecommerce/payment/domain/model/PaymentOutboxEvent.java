package com.monat.ecommerce.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Outbox Event domain model.
 * Used for Transactional Outbox Pattern to ensure reliable event delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutboxEvent {

    private UUID id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    
    @Builder.Default
    private Boolean processed = false;
    
    @Builder.Default
    private Integer retryCount = 0;
    
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private Long version;

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
    
    public void incrementRetry() {
        this.retryCount++;
    }
}
