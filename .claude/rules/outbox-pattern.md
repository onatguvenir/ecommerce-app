# Transactional Outbox Pattern — Mandatory Rules

Cross-reference: `KAFKA-GUIDE.md` for broker configuration details.

## The Rule
**Never call `KafkaTemplate.send()` directly from a service method that also writes to the database.**

This causes dual-write problems: if Kafka publish succeeds but the DB transaction rolls back (or vice versa), the system enters an inconsistent state.

## Correct Implementation

### Step 1 — Write event to `outbox_events` table in the same DB transaction:
```java
@Transactional
public void createOrder(CreateOrderCommand cmd) {
    Order order = orderRepository.save(new Order(cmd));
    OutboxEvent event = new OutboxEvent(
        "order-created",
        objectMapper.writeValueAsString(new OrderCreatedEvent(order.getId()))
    );
    outboxRepository.save(event);
    // Kafka NOT called here
}
```

### Step 2 — A separate `@Scheduled` poller reads unprocessed outbox events and publishes them:
```java
@Scheduled(fixedDelay = 1000)
@Transactional
public void publishOutboxEvents() {
    List<OutboxEvent> events = outboxRepository.findUnprocessed();
    for (OutboxEvent event : events) {
        kafkaTemplate.send(event.getTopic(), event.getPayload());
        event.markProcessed();
    }
}
```

## OutboxEvent Entity Requirements
- `id`: UUID, primary key
- `topic`: String — the Kafka topic name
- `payload`: String (JSON)
- `processedAt`: Instant, nullable — null means unprocessed
- `createdAt`: Instant

## Anti-Patterns
- Do NOT publish in the same `@Transactional` method that saves domain entities.
- Do NOT delete outbox events after processing — mark them as processed for auditability.
- Do NOT process outbox events without `@Transactional` on the poller — ensures at-least-once delivery.
