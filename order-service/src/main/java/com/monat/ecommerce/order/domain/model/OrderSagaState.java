package com.monat.ecommerce.order.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saga state for tracking order processing workflow
 */
@Entity
@Table(name = "order_saga_state")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 50)
    @Builder.Default
    private SagaStep currentStep = SagaStep.ORDER_CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void moveToNextStep(SagaStep nextStep) {
        this.currentStep = nextStep;
    }

    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
    }

    public void markAsFailed(String error) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = error;
    }

    public void markAsCompensating() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void markAsCompensated() {
        this.status = SagaStatus.COMPENSATED;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
