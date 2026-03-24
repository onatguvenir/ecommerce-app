package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer for consuming payment events.
 *
 * Design Note:
 *  PaymentCompletedEvent and PaymentFailedEvent do not contain a userId field.
 *  As a result, User Service calls cannot be made. Customer notifications are mainly 
 *  handled via OrderEventConsumer (order.completed, order.cancelled).
 *  The payment consumer is retained for logging, auditing, and internal processing 
 *  under DLQ / Idempotency protection.
 *
 *  Production-Grade Features:
 *   - Idempotency protection (via ProcessedEvent table).
 *   - Exception handling → triggers KafkaConsumerConfig's DefaultErrorHandler (DLQ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "payment.completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_COMPLETED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: paymentId={}, type={}", eventKey, eventType);
            return;
        }

        log.info("Payment completed: paymentId={}, orderId={}, amount={}",
                event.getPaymentId(), event.getOrderId(), event.getAmount());

        // Customer notification is dispatched via OrderEventConsumer > order.completed event.
        // This consumer acts solely for internal record keeping / auditing purposes.

        markAsProcessed(eventKey, eventType);
    }

    @Transactional
    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_FAILED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: paymentId={}, type={}", eventKey, eventType);
            return;
        }

        // Payment failed — order.cancelled consumer will trigger when the order cancellation event arrives
        log.warn("Payment failed: paymentId={}, orderId={}, reason={}",
                event.getPaymentId(), event.getOrderId(), event.getFailureReason());

        markAsProcessed(eventKey, eventType);
    }

    private boolean isAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
    }

    private void markAsProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.of(eventId, eventType));
    }
}
