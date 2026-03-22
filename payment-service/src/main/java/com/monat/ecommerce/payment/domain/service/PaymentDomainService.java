package com.monat.ecommerce.payment.domain.service;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import com.monat.ecommerce.payment.domain.model.PaymentOutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Payment domain service with idempotency handling and payment simulation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Value("${application.payment.failure-rate:0.30}")
    private double failureRate;

    @Value("${application.payment.processing-delay-ms:500}")
    private long processingDelayMs;

    /**
     * Process payment with idempotency support
     * If the idempotency key already exists, return the existing payment
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
        retryFor = {PessimisticLockingFailureException.class, CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public Payment processPayment(
            String idempotencyKey,
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod) {

        log.info("Processing payment - Idempotency Key: {}, Order: {}, Amount: {}",
                idempotencyKey, orderId, amount);

        // Check idempotency with PESSIMISTIC_WRITE lock
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKeyWithLock(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Payment already processed (idempotent) - Returning existing payment: {}",
                    existingPayment.get().getId());
            return existingPayment.get();
        }

        // Create payment record
        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);

        // Simulate payment processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate payment processing (random success/failure based on failure rate)
        boolean paymentSuccessful = random.nextDouble() > failureRate;

        if (paymentSuccessful) {
            // Successful payment
            String paymentReference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.markAsSuccessful(paymentReference);
            payment = paymentRepository.save(payment);

            log.info("Payment successful - Payment ID: {}, Reference: {}", payment.getId(), paymentReference);

            // Publish PaymentCompletedEvent
            publishPaymentCompletedEvent(payment);

        } else {
            // Failed payment
            String failureReason = simulateFailureReason();
            payment.markAsFailed(failureReason);
            payment = paymentRepository.save(payment);

            log.warn("Payment failed - Payment ID: {}, Reason: {}", payment.getId(), failureReason);

            // Publish PaymentFailedEvent
            publishPaymentFailedEvent(payment);
        }

        return payment;
    }

    /**
     * Refund payment (compensation)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
        retryFor = {PessimisticLockingFailureException.class, CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public Payment refundPayment(String paymentId, String orderId, BigDecimal amount, String reason) {
        log.info("Refunding payment - Payment ID: {}, Amount: {}", paymentId, amount);

        // Find payment by ID or order ID
        Payment payment;
        try {
            UUID uuid = UUID.fromString(paymentId);
            payment = paymentRepository.findByIdWithLock(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        } catch (IllegalArgumentException e) {
        // Try finding by order ID since refund via orderId is needed
            payment = paymentRepository.findByOrderId(orderId).stream()
                    .filter(Payment::canBeRefunded)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No refundable payment found for order: " + orderId));
            
            // Acquire lock since we have the ID now
            payment = paymentRepository.findByIdWithLock(payment.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment lock failed"));
        }

        if (!payment.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        // Simulate refund processing
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String refundReference = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.markAsRefunded(refundReference, amount);
        payment = paymentRepository.save(payment);

        log.info("Refund successful - Payment ID: {}, Refund Reference: {}", payment.getId(), refundReference);

        return payment;
    }

    /**
     * Get payment status
     */
    @Transactional(readOnly = true)
    public Payment getPaymentStatus(String paymentId) {
        UUID uuid = UUID.fromString(paymentId);
        return paymentRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    private String simulateFailureReason() {
        String[] reasons = {
                "Insufficient funds",
                "Card declined",
                "Invalid card number",
                "Card expired",
                "Payment gateway timeout",
                "Transaction limit exceeded"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent
                    .builder()
                    .paymentId(payment.getId().toString())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .paymentReference(payment.getPaymentReference())
                    .paymentMethod(payment.getPaymentMethod().name())
                    .build();

            saveOutboxEvent("PaymentCompleted", payment.getOrderId(), event);
            log.info("Published PaymentCompletedEvent for order: {}", payment.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent", e);
        }
    }

    private void publishPaymentFailedEvent(Payment payment) {
        try {
            PaymentFailedEvent event = PaymentFailedEvent
                    .builder()
                    .paymentId(payment.getId().toString())
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .failureReason(payment.getFailureReason())
                    .build();

            saveOutboxEvent("PaymentFailed", payment.getOrderId(), event);
            log.info("Published PaymentFailedEvent for order: {}", payment.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent", e);
        }
    }

    private void saveOutboxEvent(String eventType, String aggregateId, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            PaymentOutboxEvent outboxEvent = PaymentOutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("Payment")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .processed(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
            paymentRepository.saveOutboxEvent(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event for aggregate: " + aggregateId, e);
            throw new RuntimeException("Could not save outbox event", e);
        }
    }
}
