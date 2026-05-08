---
type: ai-playbook
topic: add-kafka-event
last-updated: 2026-04-27
---

# Playbook: Add a New Kafka Event

## Step 1 — Define Event Model (event-models module)
```java
// event-models/src/main/java/com/monat/ecommerce/events/<domain>/MyNewEvent.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyNewEvent extends BaseEvent {
    private UUID entityId;
    private String someField;
    // all fields Kafka-serializable (String, UUID, BigDecimal, Instant)
}
```

## Step 2 — Add Outbox Entry in Publisher Service
In the service method that triggers the event (within `@Transactional`):
```java
MyNewEvent event = MyNewEvent.builder()
    .entityId(entity.getId())
    .someField(entity.getSomeField())
    .build();

String payload = objectMapper.writeValueAsString(event);

OutboxEvent outboxEvent = OutboxEvent.builder()
    .aggregateType("MyEntity")
    .aggregateId(entity.getId().toString())
    .eventType("MyNewEvent")
    .payload(payload)
    .build();

outboxEventRepository.save(outboxEvent);
// NO kafkaTemplate.send() here
```

## Step 3 — Verify Outbox Poller Picks Up New EventType
The `@Scheduled OutboxEventPublisher` reads all unprocessed outbox rows regardless of event type.  
Outbox poller sends to `outboxEvent.getTopic()` — ensure `aggregateType` or `eventType` maps to the correct Kafka topic name.  
Check: `OutboxEventPublisher` uses `outboxEvent.getTopic()` — set topic name correctly in Step 2.

## Step 4 — Create Kafka Consumer in Subscriber Service
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MyNewEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "my-new-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handle(MyNewEvent event) {
        String eventKey = event.getEntityId().toString();
        String eventType = "MY_NEW_EVENT";

        if (processedEventRepository.existsByEventIdAndEventType(eventKey, eventType)) {
            log.warn("Duplicate event skipped: {}", eventKey);
            return;
        }

        // ... process event ...

        processedEventRepository.save(ProcessedEvent.of(eventKey, eventType));
    }
}
```

## Step 5 — Add Topic to application.yml (if needed)
```yaml
spring:
  kafka:
    producer:
      # topic auto-created by Kafka — no explicit config needed for dev
```

## Checklist
- [ ] Event class in event-models (extends BaseEvent)
- [ ] Outbox row saved in same `@Transactional` method as entity save
- [ ] NO direct `kafkaTemplate.send()` in service layer
- [ ] Consumer has idempotency check via `processedEventRepository`
- [ ] Consumer annotated `@Transactional`
- [ ] Topic name consistent across publisher outbox and consumer `@KafkaListener`
