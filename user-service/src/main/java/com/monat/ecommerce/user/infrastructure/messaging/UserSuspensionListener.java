package com.monat.ecommerce.user.infrastructure.messaging;

import com.monat.ecommerce.events.fraud.UserAccountSuspendedEvent;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka listener that consumes {@link UserAccountSuspendedEvent} published by the fraud-service.
 *
 * Theory (Educational Note):
 * This listener bridges the event-driven boundary between the fraud-service and
 * the user-service. The fraud-service does NOT call user-service directly (no REST call),
 * following the principles of loose coupling and autonomy in microservices.
 *
 * The actual suspension uses a Pessimistic Write Lock inside UserRepositoryImpl to ensure
 * that if the same event is delivered more than once (Kafka at-least-once delivery guarantee),
 * the second invocation is safely ignored (idempotent behaviour via the SUSPENDED status check).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSuspensionListener {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "${app.user-suspension.topic:user-suspension-events}",
            groupId = "user-service-suspension",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserAccountSuspended(UserAccountSuspendedEvent event, Acknowledgment ack) {
        log.warn("Received UserAccountSuspendedEvent for userId={}, reason={}",
                event.userId(), event.reason());

        try {
            UUID userId = UUID.fromString(event.userId());
            userRepository.suspendUserById(userId, event.reason());
            // Acknowledge ONLY after successful processing (manual ack mode)
            ack.acknowledge();
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format in UserAccountSuspendedEvent: {}", event.userId(), e);
            // Acknowledge to avoid endless retry on a malformed event - route to DLQ if necessary
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process UserAccountSuspendedEvent for userId={}", event.userId(), e);
            // Do NOT acknowledge — Kafka will redeliver (retry semantics)
        }
    }
}
