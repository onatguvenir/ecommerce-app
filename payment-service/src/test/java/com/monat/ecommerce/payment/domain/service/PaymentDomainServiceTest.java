package com.monat.ecommerce.payment.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.model.PaymentOutboxEvent;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import com.monat.ecommerce.payment.infrastructure.config.PaymentMetrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentDomainService} outbox handling.
 *
 * <p>Regression guard for the saga-failing bug where a manually-assigned id on the
 * {@code @GeneratedValue}/{@code @Version} {@code PaymentOutboxEventEntity} made Hibernate
 * treat the row as a detached entity ("uninitialized version value 'null'"), which marked
 * the transaction rollback-only and surfaced as a gRPC INTERNAL — failing the whole order.
 */
@ExtendWith(MockitoExtension.class)
class PaymentDomainServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentMetrics paymentMetrics;

    private PaymentDomainService service;

    @BeforeEach
    void setUp() {
        service = new PaymentDomainService(paymentRepository, objectMapper, paymentMetrics);
        // Force the deterministic "payment successful" branch and skip the simulated delay.
        ReflectionTestUtils.setField(service, "failureRate", -1.0);
        ReflectionTestUtils.setField(service, "processingDelayMs", 0L);
    }

    @Test
    @DisplayName("outbox event is built without a manual id so the entity persists (not detached)")
    void outboxEventHasNoManuallyAssignedId() throws Exception {
        Payment saved = Payment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("idem-1")
                .orderId("order-1")
                .userId("user-1")
                .amount(BigDecimal.valueOf(14.99))
                .currency("USD")
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.PROCESSING)
                .build();

        when(paymentRepository.findByIdempotencyKeyWithLock("idem-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(paymentMetrics.paymentProcessingTimer())
                .thenReturn(Timer.builder("test.payment.processing").register(new SimpleMeterRegistry()));

        ArgumentCaptor<PaymentOutboxEvent> captor = ArgumentCaptor.forClass(PaymentOutboxEvent.class);

        service.processPayment("idem-1", "order-1", "user-1",
                BigDecimal.valueOf(14.99), "USD", PaymentMethod.CARD);

        verify(paymentRepository).saveOutboxEvent(captor.capture());
        PaymentOutboxEvent outbox = captor.getValue();

        assertThat(outbox.getId())
                .as("id must stay null so Hibernate @GeneratedValue assigns it; a manual id on a "
                        + "@Version entity is treated as detached and fails the persist")
                .isNull();
        assertThat(outbox.getEventType()).isEqualTo("PaymentCompleted");
        assertThat(outbox.getAggregateId()).isEqualTo("order-1");
    }
}
