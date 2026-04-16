package com.monat.ecommerce.payment.application.service;

import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.application.dto.ProcessPaymentRequest;
import com.monat.ecommerce.payment.application.mapper.PaymentDtoMapper;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentApplicationService
 */
@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private PaymentDtoMapper paymentMapper;

        @InjectMocks
        private PaymentApplicationService paymentApplicationService;

        private ProcessPaymentRequest processPaymentRequest;
        private Payment payment;
        private PaymentResponse paymentResponse;
        private UUID paymentId;

        @BeforeEach
        void setUp() {
                paymentId = UUID.randomUUID();

                processPaymentRequest = ProcessPaymentRequest.builder()
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .currency("USD")
                                .paymentMethod("CARD")
                                .idempotencyKey("IDEM-123")
                                .build();

                payment = Payment.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .currency("USD")
                                .paymentMethod(PaymentMethod.CARD)
                                .status(PaymentStatus.PENDING)
                                .idempotencyKey("IDEM-123")
                                .build();

                paymentResponse = PaymentResponse.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .currency("USD")
                                .paymentMethod("CARD")
                                .status(PaymentStatus.PENDING.name())
                                .build();
        }

        @Test
        void processPayment_Success() {
                // Given
                when(paymentRepository.findByIdempotencyKey("IDEM-123")).thenReturn(Optional.empty());
                when(paymentMapper.toPayment(processPaymentRequest)).thenReturn(payment);
                when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
                when(paymentMapper.toResponse(any(Payment.class))).thenReturn(paymentResponse);

                // When
                PaymentResponse response = paymentApplicationService.processPayment(processPaymentRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.orderId()).isEqualTo("ORDER-123");
                verify(paymentRepository, times(1)).findByIdempotencyKey("IDEM-123");
                verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        void processPayment_IdempotentRequest() {
                // Given - Payment already exists with same idempotency key
                Payment existingPayment = Payment.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .status(PaymentStatus.COMPLETED)
                                .idempotencyKey("IDEM-123")
                                .build();

                when(paymentRepository.findByIdempotencyKey("IDEM-123"))
                                .thenReturn(Optional.of(existingPayment));
                when(paymentMapper.toResponse(existingPayment)).thenReturn(paymentResponse);

                // When
                PaymentResponse response = paymentApplicationService.processPayment(processPaymentRequest);

                // Then
                assertThat(response).isNotNull();
                verify(paymentRepository, times(1)).findByIdempotencyKey("IDEM-123");
                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        void getPayment_Found() {
                // Given
                when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
                when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

                // When
                PaymentResponse response = paymentApplicationService.getPayment(paymentId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.orderId()).isEqualTo("ORDER-123");
                verify(paymentRepository, times(1)).findById(paymentId);
        }

        @Test
        void getPayment_NotFound() {
                // Given
                UUID randomId = UUID.randomUUID();
                when(paymentRepository.findById(randomId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> paymentApplicationService.getPayment(randomId))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Payment not found");

                verify(paymentRepository, times(1)).findById(randomId);
        }

        @Test
        void getPaymentByOrderId_Found() {
                // Given
                when(paymentRepository.findByOrderId("ORDER-123")).thenReturn(List.of(payment));
                when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

                // When
                PaymentResponse response = paymentApplicationService.getPaymentByOrderId("ORDER-123");

                // Then
                assertThat(response).isNotNull();
                assertThat(response.orderId()).isEqualTo("ORDER-123");
                verify(paymentRepository, times(1)).findByOrderId("ORDER-123");
        }

        @Test
        void getPaymentByOrderId_NotFound() {
                // Given
                when(paymentRepository.findByOrderId("INVALID")).thenReturn(Collections.emptyList());

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
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .status(PaymentStatus.COMPLETED)
                                .build();

                Payment refundedPayment = Payment.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .status(PaymentStatus.REFUNDED)
                                .build();

                PaymentResponse refundedResponse = PaymentResponse.builder()
                                .orderId("ORDER-123")
                                .status(PaymentStatus.REFUNDED.name())
                                .build();

                when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(completedPayment));
                when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
                when(paymentMapper.toResponse(any(Payment.class))).thenReturn(refundedResponse);

                // When
                PaymentResponse response = paymentApplicationService.refundPayment(paymentId);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED.name());
                verify(paymentRepository, times(1)).findByIdWithLock(paymentId);
                verify(paymentRepository, times(1)).save(any(Payment.class));
        }

        @Test
        void refundPayment_AlreadyRefunded() {
                // Given
                Payment refundedPayment = Payment.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .status(PaymentStatus.REFUNDED)
                                .build();

                when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(refundedPayment));

                // When & Then
                assertThatThrownBy(() -> paymentApplicationService.refundPayment(paymentId))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("already refunded");

                verify(paymentRepository, never()).save(any());
        }

        @Test
        void refundPayment_NotCompleted() {
                // Given
                Payment pendingPayment = Payment.builder()
                                .id(paymentId)
                                .orderId("ORDER-123")
                                .amount(BigDecimal.valueOf(199.99))
                                .status(PaymentStatus.PENDING)
                                .build();

                when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(pendingPayment));

                // When & Then
                assertThatThrownBy(() -> paymentApplicationService.refundPayment(paymentId))
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
                                .paymentMethod("CARD")
                                .idempotencyKey("IDEM-456")
                                .build();

                // When & Then
                assertThatThrownBy(() -> paymentApplicationService.processPayment(invalidRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Payment amount must be positive");

                verify(paymentRepository, never()).save(any());
        }
}
