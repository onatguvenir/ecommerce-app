package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private String paymentId;
    private String orderId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID().toString();
        orderId = UUID.randomUUID().toString();
    }

    @Test
    void handlePaymentCompleted_ShouldProcessSuccessfully() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .amount(new BigDecimal("150.00"))
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(paymentId, "PAYMENT_COMPLETED"))
                .thenReturn(false);

        // Act
        paymentEventConsumer.handlePaymentCompleted(event);

        // Assert
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void handlePaymentCompleted_ShouldSkipIfAlreadyProcessed() {
        // Arrange
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(paymentId, "PAYMENT_COMPLETED"))
                .thenReturn(true);

        // Act
        paymentEventConsumer.handlePaymentCompleted(event);

        // Assert
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    void handlePaymentFailed_ShouldProcessSuccessfully() {
        // Arrange
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .failureReason("Insufficient funds")
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(paymentId, "PAYMENT_FAILED"))
                .thenReturn(false);

        // Act
        paymentEventConsumer.handlePaymentFailed(event);

        // Assert
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void handlePaymentFailed_ShouldSkipIfAlreadyProcessed() {
        // Arrange
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .build();

        when(processedEventRepository.existsByEventIdAndEventType(paymentId, "PAYMENT_FAILED"))
                .thenReturn(true);

        // Act
        paymentEventConsumer.handlePaymentFailed(event);

        // Assert
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
