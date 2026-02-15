package com.monat.ecommerce.payment.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.application.dto.ProcessPaymentRequest;
import com.monat.ecommerce.payment.application.mapper.PaymentDtoMapper;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Application Service.
 * <p>
 * This class orchestrates payment processing, idempotency checks, and gateway
 * interactions.
 * </p>
 * 
 * @Service indicates that this class is a "Service" component containing
 *          business logic.
 * 
 * @Transactional ensures that payment state changes are atomic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentDtoMapper paymentMapper;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        if (request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPayment.isPresent()) {
            log.info("Payment with idempotency key {} already processed", request.getIdempotencyKey());
            return paymentMapper.toResponse(existingPayment.get());
        }

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Simulate payment processing logic (e.g., call external gateway)
        // For now, simple logic: succeed.
        payment.markAsSuccessful(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .findFirst() // Assuming one payment per order for simplicity
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + orderId));
    }

    @Transactional
    public PaymentResponse refundPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
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
}
