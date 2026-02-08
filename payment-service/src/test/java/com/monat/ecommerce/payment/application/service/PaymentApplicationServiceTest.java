package com.monat.ecommerce.payment.application.service;

import com.monat.ecommerce.payment.application.dto.ProcessPaymentRequest;
import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentApplicationService
 */
@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentApplicationService paymentApplicationService;

    private ProcessPaymentRequest processPaymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        processPaymentRequest = ProcessPaymentRequest.builder()
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .paymentMethod("CREDIT_CARD")
                .idempotencyKey("IDEM-123")
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .idempotencyKey("IDEM-123")
                .build();
    }

    @Test
    void processPayment_Success() {
        // Given
        when(paymentRepository.findByIdempotencyKey("IDEM-123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        PaymentResponse response = paymentApplicationService.processPayment(processPaymentRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("ORDER-123");
        assertThat(response.getStatus()).isIn(PaymentStatus.COMPLETED.name(), PaymentStatus.FAILED.name());
        verify(paymentRepository, times(1)).findByIdempotencyKey("IDEM-123");
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_IdempotentRequest() {
        // Given - Payment already exists with same idempotency key
        Payment existingPayment = Payment.builder()
                .id(1L)
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .status(PaymentStatus.COMPLETED)
                .idempotencyKey("IDEM-123")
                .build();

        when(paymentRepository.findByIdempotencyKey("IDEM-123"))
                .thenReturn(Optional.of(existingPayment));

        // When
        PaymentResponse response = paymentApplicationService.processPayment(processPaymentRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());
        verify(paymentRepository, times(1)).findByIdempotencyKey("IDEM-123");
        verify(paymentRepository, never()).save(any(Payment.class)); // Should not save again
    }

    @Test
    void getPayment_Found() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentApplicationService.getPayment(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo("ORDER-123");
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void getPayment_NotFound() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.getPayment(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");

        verify(paymentRepository, times(1)).findById(999L);
    }

    @Test
    void getPaymentByOrderId_Found() {
        // Given
        when(paymentRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(payment));

        // When
        PaymentResponse response = paymentApplicationService.getPaymentByOrderId("ORDER-123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("ORDER-123");
        verify(paymentRepository, times(1)).findByOrderId("ORDER-123");
    }

    @Test
    void getPaymentByOrderId_NotFound() {
        // Given
        when(paymentRepository.findByOrderId("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.getPaymentByOrderId("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found for order");

        verify(paymentRepository, times(1)).findByOrderId("INVALID");
    }

    @Test
    void refundPayment_Success() {
        // Given
        Payment completedPayment = Payment.builder()
                .id(1L)
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(completedPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(completedPayment);

        // When
        PaymentResponse response = paymentApplicationService.refundPayment(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void refundPayment_AlreadyRefunded() {
        // Given
        Payment refundedPayment = Payment.builder()
                .id(1L)
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .status(PaymentStatus.REFUNDED)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(refundedPayment));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already refunded");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_NotCompleted() {
        // Given
        Payment pendingPayment = Payment.builder()
                .id(1L)
                .orderId("ORDER-123")
                .amount(BigDecimal.valueOf(199.99))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be refunded");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_ValidatesAmount() {
        // Given
        ProcessPaymentRequest invalidRequest = ProcessPaymentRequest.builder()
                .orderId("ORDER-123")
                .amount(BigDecimal.ZERO) // Invalid amount
                .paymentMethod("CREDIT_CARD")
                .idempotencyKey("IDEM-456")
                .build();

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.processPayment(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");

        verify(paymentRepository, never()).save(any());
    }
}
