package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;
import com.monat.ecommerce.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "payment.completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: {}", event.getPaymentId());

        try {
            // Note: We'd typically fetch user email from User Service
            // For now, this is handled by order.completed event
            log.info("Payment completed notification for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to send payment completed notification", e);
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: {}", event.getPaymentId());

        try {
            log.warn("Payment failed for order: {} - Reason: {}", event.getOrderId(), event.getFailureReason());
            // Could send payment failure notification here

        } catch (Exception e) {
            log.error("Failed to process payment failed event", e);
        }
    }
}
