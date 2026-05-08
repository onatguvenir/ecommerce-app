---
type: ai-example
topic: transactional-outbox
last-updated: 2026-04-27
---

# Example: Complete Transactional Outbox Implementation

Based on actual implementation in order-service.

## 1. Outbox Entity (domain/model/OutboxEvent.java)

```java
@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;   // "Order", "Payment"

    @Column(nullable = false)
    private String aggregateId;     // entity UUID as string

    @Column(nullable = false)
    private String eventType;       // "OrderCompleted", "PaymentFailed"

    @Column(nullable = false, columnDefinition = "text")
    private String payload;         // JSON string

    @Column
    private Instant processedAt;    // null = unprocessed

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
}
```

## 2. Outbox Repository

```java
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop100ByProcessedAtIsNullOrderByCreatedAtAsc();
}
```

## 3. Save to Outbox (same @Transactional as entity save)

```java
@Transactional
public void createOrder(CreateOrderRequest request) {
    Order order = orderRepository.save(new Order(...));

    OrderCreatedEvent event = OrderCreatedEvent.builder()
        .orderId(order.getId().toString())
        .orderNumber(order.getOrderNumber())
        .userId(order.getUserId().toString())
        .totalAmount(order.getTotalAmount())
        .build();

    OutboxEvent outboxEvent = OutboxEvent.builder()
        .aggregateType("Order")
        .aggregateId(order.getId().toString())
        .eventType("OrderCreated")
        .payload(objectMapper.writeValueAsString(event))
        .build();

    outboxEventRepository.save(outboxEvent);
    // Both saves in same transaction — atomic
}
```

## 4. Outbox Poller (@Scheduled publisher)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByProcessedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            try {
                String topic = resolveTopicName(event.getEventType());
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                // Will retry on next poll cycle
            }
        }
    }

    private String resolveTopicName(String eventType) {
        return switch (eventType) {
            case "OrderCreated"   -> "order.created";
            case "OrderCompleted" -> "order.completed";
            case "OrderCancelled" -> "order.cancelled";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
```

## 5. Flyway Migration for Outbox Table

```sql
-- V3__create_outbox_events.sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events (created_at)
    WHERE processed_at IS NULL;
```
