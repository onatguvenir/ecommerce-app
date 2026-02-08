package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.order.OrderCompletedEvent;
import com.monat.ecommerce.events.order.OrderCreatedEvent;
import com.monat.ecommerce.events.order.OrderCancelledEvent;
import com.monat.ecommerce.notification.domain.service.EmailService;
import com.monat.ecommerce.notification.domain.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;
    private final SmsService smsService;

    @KafkaListener(topics = "order.created", groupId = "notification-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event.getOrderId());

        try {
            // TODO: Fetch user details from User Service using event.getUserId()
            // For now, just log the event
            log.info("Order confirmation for user {}, order: {}, total: {}",
                    event.getUserId(),
                    event.getOrderNumber(),
                    event.getTotalAmount());

            log.info("Order confirmation notifications queued for order: {}", event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to process order created event", e);
        }
    }

    @KafkaListener(topics = "order.completed", groupId = "notification-service-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Received OrderCompletedEvent: {}", event.getOrderId());

        try {
            // TODO: Fetch user details from User Service using event.getUserId()
            log.info("Order completed for user {}, order: {}",
                    event.getUserId(),
                    event.getOrderNumber());

            log.info(" Order completed notifications queued for order: {}", event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to process order completed event", e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-service-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: {}", event.getOrderId());

        try {
            // TODO: Fetch user details from User Service using event.getUserId()
            log.info("Order cancelled for user {}, order: {}, reason: {}",
                    event.getUserId(),
                    event.getOrderNumber(),
                    event.getReason());

            log.info("Order cancellation notifications queued for order: {}", event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to process order cancelled event", e);
        }
    }
}
