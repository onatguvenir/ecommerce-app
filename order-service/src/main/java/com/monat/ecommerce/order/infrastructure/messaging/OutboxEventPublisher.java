package com.monat.ecommerce.order.infrastructure.messaging;

import com.monat.ecommerce.order.domain.model.OutboxEvent;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox event publisher - polls outbox table and publishes to Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${application.outbox.polling-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        int batchSize = 100;
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = getTopicForEventType(event.getEventType());
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());

                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.debug("Published event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                // Event will be retried in next poll
            }
        }
    }

    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "OrderCreated" -> "order.created";
            case "OrderCompleted" -> "order.completed";
            case "OrderCancelled" -> "order.cancelled";
            default -> "order.events";
        };
    }
}
