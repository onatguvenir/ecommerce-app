package com.monat.ecommerce.payment.domain.service;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    @Value("${application.payment.failure-rate:0.30}")
    private double failureRate;

    @Value("${application.payment.processing-delay-ms:500}")
    private long processingDelayMs;

    /**
     * Process payment with idempotency support
     * If the idempotency key already exists, return the existing payment
     */
    @Transactional
    public Payment processPayment(
            String idempotencyKey,
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod) {

        log.info("Processing payment - Idempotency Key: {}, Order: {}, Amount: {}",
                idempotencyKey, orderId, amount);

        // Check idempotency
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
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
    @Transactional
    public Payment refundPayment(String paymentId, String orderId, BigDecimal amount, String reason) {
        log.info("Refunding payment - Payment ID: {}, Amount: {}", paymentId, amount);

        // Find payment by ID or order ID
        Payment payment;
        try {
            UUID uuid = UUID.fromString(paymentId);
            payment = paymentRepository.findById(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        } catch (IllegalArgumentException e) {
            // Try finding by order ID
            payment = paymentRepository.findByOrderId(orderId).stream()
                    .filter(Payment::canBeRefunded)
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException("No refundable payment found for order: " + orderId));
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
            com.monat.ecommerce.events.payment.PaymentCompletedEvent event = com.monat.ecommerce.events.payment.PaymentCompletedEvent
                    .builder()
                    .paymentId(payment.getId().toString())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .paymentReference(payment.getPaymentReference())
                    .paymentMethod(payment.getPaymentMethod().name())
                    .build();

            kafkaTemplate.send("payment.completed", payment.getOrderId(), event);
            log.info("Published PaymentCompletedEvent for order: {}", payment.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent", e);
        }
    }

    private void publishPaymentFailedEvent(Payment payment) {
        try {
            com.monat.ecommerce.events.payment.PaymentFailedEvent event = com.monat.ecommerce.events.payment.PaymentFailedEvent
                    .builder()
                    .paymentId(payment.getId().toString())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .failureReason(payment.getFailureReason())
                    .build();

            kafkaTemplate.send("payment.failed", payment.getOrderId(), event);
            log.info("Published PaymentFailedEvent for order: {}", payment.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent", e);
        }
    }
}
