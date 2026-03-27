package com.monat.ecommerce.payment.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.application.dto.ProcessPaymentRequest;
import com.monat.ecommerce.payment.application.mapper.PaymentDtoMapper;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinator for Payment-related business logic.
 * 
 * Educational Note:
 * - Isolation.SERIALIZABLE: Used to prevent any concurrency anomalies during 
 *   payment processing (e.g., Phantom reads, Non-repeatable reads).
 * - RateLimiter: Protects the service from burst traffic.
 * - Idempotency: Ensures that the same payment request isn't processed twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentDtoMapper paymentMapper;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
        retryFor = {PessimisticLockingFailureException.class, CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @RateLimiter(name = "payment-api", fallbackMethod = "processPaymentRateLimitFallback")
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.info("Payment with idempotency key {} already processed", request.idempotencyKey());
            return paymentMapper.toResponse(existingPayment.get());
        }

        // Create payment using MapStruct
        Payment payment = paymentMapper.toPayment(request);
        payment.setId(UUID.randomUUID());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // Simulate payment processing logic (e.g., call external gateway)
        // For now, simple logic: succeed.
        payment.markAsSuccessful(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    @Observed(name = "payment.lookup", contextualName = "payment-get-by-id")
    public PaymentResponse getPayment(UUID id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Observed(name = "payment.lookup", contextualName = "payment-get-by-order-id")
    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .findFirst() // Assuming one payment per order for simplicity
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + orderId));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
        retryFor = {PessimisticLockingFailureException.class, CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @RateLimiter(name = "payment-api", fallbackMethod = "refundPaymentRateLimitFallback")
    public PaymentResponse refundPayment(UUID id) {
        Payment payment = paymentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment already refunded");
        }

        if (!payment.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in status: " + payment.getStatus());
        }

        // Simulate refund logic
        payment.markAsRefunded(UUID.randomUUID().toString(), payment.getAmount());

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }
    
    // Fallback methods for RateLimiter
    private PaymentResponse processPaymentRateLimitFallback(ProcessPaymentRequest request, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for processPayment: orderId={}", request.orderId());
        throw new IllegalStateException("Too many requests to payment service, please try again later.");
    }

    private PaymentResponse refundPaymentRateLimitFallback(UUID id, RequestNotPermitted ex) {
        log.warn("Rate limit exceeded for refundPayment: paymentId={}", id);
        throw new IllegalStateException("Too many requests to payment service, please try again later.");
    }
}
