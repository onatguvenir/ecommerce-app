package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.order.OrderCancelledEvent;
import com.monat.ecommerce.events.order.OrderCompletedEvent;
import com.monat.ecommerce.events.order.OrderCreatedEvent;
import com.monat.ecommerce.grpc.user.User;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.domain.service.EmailService;
import com.monat.ecommerce.notification.domain.service.SmsService;
import com.monat.ecommerce.notification.infrastructure.config.NotificationMetrics;
import com.monat.ecommerce.notification.infrastructure.grpc.UserServiceClient;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Kafka Consumer for Order-related events.
 *
 * Educational Note:
 * 1. Idempotency: Every event is checked against the ProcessedEvent table before 
 *    dispatching a notification to prevent duplicates (exactly-once processing).
 *
 * 2. gRPC Integration: Uses UserServiceClient (gRPC with CircuitBreaker) 
 *    to fetch user details for e-mail delivery.
 *
 * 3. DLQ (Dead Letter Queue): Unhandled exceptions are allowed to bubble up, 
 *    triggering Kafka retries and eventually moving the message to a DLT topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationMetrics notificationMetrics;

    // -------------------------------------------------------------------------
    // ORDER CREATED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.created", groupId = "notification-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        Timer.Sample sample = Timer.start();
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_CREATED";

        try {
            if (isAlreadyProcessed(eventKey, eventType)) {
                log.warn("[Idempotency] Skipping: orderId={}, type={}", eventKey, eventType);
                notificationMetrics.incrementConsumerEvent(eventType, "duplicate");
                return;
            }

            log.info("Received OrderCreatedEvent: orderId={}, orderNumber={}", event.getOrderId(), event.getOrderNumber());

            Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                emailService.sendOrderConfirmation(
                        user.getEmail(),
                        event.getOrderNumber(),
                        user.getFirstName() + " " + user.getLastName(),
                        event.getTotalAmount().toString()
                );
                log.info("Order confirmation email sent: orderId={}, email={}", eventKey, user.getEmail());
                notificationMetrics.incrementConsumerEvent(eventType, "processed");
            } else {
                log.warn("User information not found, notification skipped: userId={}", event.getUserId());
                notificationMetrics.incrementConsumerEvent(eventType, "user_missing");
            }

            markAsProcessed(eventKey, eventType);
        } catch (RuntimeException ex) {
            notificationMetrics.incrementConsumerEvent(eventType, "error");
            throw ex;
        } finally {
            sample.stop(notificationMetrics.consumerTimer());
        }
    }

    // -------------------------------------------------------------------------
    // ORDER COMPLETED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.completed", groupId = "notification-service-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Timer.Sample sample = Timer.start();
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_COMPLETED";

        try {
            if (isAlreadyProcessed(eventKey, eventType)) {
                log.warn("[Idempotency] Skipping: orderId={}, type={}", eventKey, eventType);
                notificationMetrics.incrementConsumerEvent(eventType, "duplicate");
                return;
            }

            log.info("Received OrderCompletedEvent: orderId={}", event.getOrderId());

            Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                emailService.sendOrderCompleted(
                        user.getEmail(),
                        event.getOrderNumber(),
                        user.getFirstName() + " " + user.getLastName()
                );
                log.info("Order completed email sent: orderId={}", eventKey);
                notificationMetrics.incrementConsumerEvent(eventType, "processed");
            } else {
                log.warn("User information not found, notification skipped: userId={}", event.getUserId());
                notificationMetrics.incrementConsumerEvent(eventType, "user_missing");
            }

            markAsProcessed(eventKey, eventType);
        } catch (RuntimeException ex) {
            notificationMetrics.incrementConsumerEvent(eventType, "error");
            throw ex;
        } finally {
            sample.stop(notificationMetrics.consumerTimer());
        }
    }

    // -------------------------------------------------------------------------
    // ORDER CANCELLED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.cancelled", groupId = "notification-service-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Timer.Sample sample = Timer.start();
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_CANCELLED";

        try {
            if (isAlreadyProcessed(eventKey, eventType)) {
                log.warn("[Idempotency] Skipping: orderId={}, type={}", eventKey, eventType);
                notificationMetrics.incrementConsumerEvent(eventType, "duplicate");
                return;
            }

            log.info("Received OrderCancelledEvent: orderId={}, reason={}", event.getOrderId(), event.getReason());

            Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                emailService.sendOrderCancelled(
                        user.getEmail(),
                        event.getOrderNumber(),
                        user.getFirstName() + " " + user.getLastName(),
                        event.getReason()
                );
                log.info("Order cancelled email sent: orderId={}", eventKey);
                notificationMetrics.incrementConsumerEvent(eventType, "processed");
            } else {
                log.warn("User information not found, notification skipped: userId={}", event.getUserId());
                notificationMetrics.incrementConsumerEvent(eventType, "user_missing");
            }

            markAsProcessed(eventKey, eventType);
        } catch (RuntimeException ex) {
            notificationMetrics.incrementConsumerEvent(eventType, "error");
            throw ex;
        } finally {
            sample.stop(notificationMetrics.consumerTimer());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
    }

    private void markAsProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.of(eventId, eventType));
    }
}
