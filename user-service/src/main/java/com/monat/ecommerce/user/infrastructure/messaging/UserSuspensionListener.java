package com.monat.ecommerce.user.infrastructure.messaging;

import com.monat.ecommerce.events.fraud.UserAccountSuspendedEvent;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import com.monat.ecommerce.user.infrastructure.config.UserMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka listener that consumes {@link UserAccountSuspendedEvent} published by the fraud-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSuspensionListener {

    private final UserRepository userRepository;
    private final UserMetrics userMetrics;

    @KafkaListener(
            topics = "${app.user-suspension.topic:user-suspension-events}",
            groupId = "user-service-suspension",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserAccountSuspended(UserAccountSuspendedEvent event, Acknowledgment ack) {
        Timer.Sample sample = Timer.start();
        log.warn("Received UserAccountSuspendedEvent for userId={}, reason={}",
                event.userId(), event.reason());

        try {
            UUID userId = UUID.fromString(event.userId());
            userRepository.suspendUserById(userId, event.reason());
            ack.acknowledge();
            userMetrics.incrementSuspensionEvent("processed");
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format in UserAccountSuspendedEvent: {}", event.userId(), e);
            ack.acknowledge();
            userMetrics.incrementSuspensionEvent("invalid_user_id");
        } catch (Exception e) {
            log.error("Failed to process UserAccountSuspendedEvent for userId={}", event.userId(), e);
            userMetrics.incrementSuspensionEvent("retryable_failure");
        } finally {
            sample.stop(userMetrics.suspensionEventTimer());
        }
    }
}
