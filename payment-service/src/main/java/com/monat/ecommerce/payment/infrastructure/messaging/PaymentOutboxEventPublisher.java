package com.monat.ecommerce.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentOutboxEventEntity;
import com.monat.ecommerce.payment.infrastructure.persistence.repository.PaymentOutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox event publisher for Payment service.
 * Polls the payment_outbox_events table and publishes pending events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxEventPublisher {

    private final PaymentOutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${application.outbox.polling-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        int batchSize = 100;
        int maxRetries = 3;
        
        List<PaymentOutboxEventEntity> pendingEvents = outboxRepository
                .findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} payment outbox events", pendingEvents.size());

        for (PaymentOutboxEventEntity event : pendingEvents) {
            try {
                if (event.getRetryCount() >= maxRetries) {
                    log.error("Event {} exceeded max retries ({}). Marking as processed to skip.", event.getId(), maxRetries);
                    event.setProcessed(true);
                    event.setProcessedAt(LocalDateTime.now());
                    outboxRepository.save(event);
                    continue; // Skip and avoid infinite retry loop mapping to DLT behavior normally
                }

                String topic = getTopicForEventType(event.getEventType());
                Object payloadObject = objectMapper.readValue(event.getPayload(), Object.class);
                
                kafkaTemplate.send(topic, event.getAggregateId(), payloadObject);

                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

                log.debug("Published event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish event: {} (Retry count: {})", event.getId(), event.getRetryCount(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepository.save(event);
            }
        }
    }

    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "PaymentCompleted" -> "payment.completed";
            case "PaymentFailed" -> "payment.failed";
            case "PaymentRefunded" -> "payment.refunded";
            default -> "payment.events";
        };
    }
}
