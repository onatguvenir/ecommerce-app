package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.infrastructure.config.NotificationMetrics;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer for consuming payment events.
 *
 * Design Note:
 * PaymentCompletedEvent and PaymentFailedEvent do not contain a userId field.
 * As a result, User Service calls cannot be made. Customer notifications are mainly
 * handled via OrderEventConsumer (order.completed, order.cancelled).
 * The payment consumer is retained for logging, auditing, and internal processing
 * under DLQ / idempotency protection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationMetrics notificationMetrics;

    @Transactional
    @KafkaListener(topics = "payment.completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Timer.Sample sample = Timer.start();
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_COMPLETED";

        try {
            if (isAlreadyProcessed(eventKey, eventType)) {
                log.warn("[Idempotency] Skipping payment event: paymentId={}, type={}", eventKey, eventType);
                notificationMetrics.incrementConsumerEvent(eventType, "duplicate");
                return;
            }

            log.info("Payment completed: paymentId={}, orderId={}, amount={}",
                    event.getPaymentId(), event.getOrderId(), event.getAmount());

            markAsProcessed(eventKey, eventType);
            notificationMetrics.incrementConsumerEvent(eventType, "processed");
        } catch (RuntimeException ex) {
            notificationMetrics.incrementConsumerEvent(eventType, "error");
            throw ex;
        } finally {
            sample.stop(notificationMetrics.consumerTimer());
        }
    }

    @Transactional
    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Timer.Sample sample = Timer.start();
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_FAILED";

        try {
            if (isAlreadyProcessed(eventKey, eventType)) {
                log.warn("[Idempotency] Skipping payment event: paymentId={}, type={}", eventKey, eventType);
                notificationMetrics.incrementConsumerEvent(eventType, "duplicate");
                return;
            }

            log.warn("Payment failed: paymentId={}, orderId={}, reason={}",
                    event.getPaymentId(), event.getOrderId(), event.getFailureReason());

            markAsProcessed(eventKey, eventType);
            notificationMetrics.incrementConsumerEvent(eventType, "processed");
        } catch (RuntimeException ex) {
            notificationMetrics.incrementConsumerEvent(eventType, "error");
            throw ex;
        } finally {
            sample.stop(notificationMetrics.consumerTimer());
        }
    }

    private boolean isAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
    }

    private void markAsProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.of(eventId, eventType));
    }
}
