---
type: ai-context
scope: backend-rules
last-updated: 2026-04-27
---

# Backend Rules

Hard rules. Violations break consistency guarantees.

## 1. Kafka Publishing — Transactional Outbox ONLY

**NEVER** call `kafkaTemplate.send()` directly in a `@Transactional` method.

```java
// WRONG — dual-write risk
@Transactional
public void createOrder(...) {
    orderRepository.save(order);
    kafkaTemplate.send("order.created", event);  // ← FORBIDDEN
}

// CORRECT — outbox pattern
@Transactional
public void createOrder(...) {
    orderRepository.save(order);
    outboxEventRepository.save(new OutboxEvent("order.created", json)); // same tx
}
// Separate @Scheduled poller reads outbox and publishes to Kafka
```

Outbox entity fields: `id (UUID)`, `aggregateType`, `aggregateId`, `eventType`, `payload (JSON)`, `processedAt (nullable Instant)`, `createdAt`.  
Poller: marks `processedAt`, never deletes rows.

## 2. Locking Strategy

### Pessimistic Write Lock (financial + stock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") UUID productId);
```
Apply to: inventory stock mutations, payment amount writes, order status transitions.

### Optimistic Lock (catalog, profiles)
```java
@Version
private Long version;
```
Apply to: product catalog, user profiles, non-financial metadata.  
Handle `OptimisticLockingFailureException` → retry or 409 Conflict.

**Never** use `synchronized` with Virtual Threads. Use `ReentrantLock`.

## 3. Transaction Boundaries

- `@Transactional` goes on **service layer** only. Never on repository or controller.
- `REQUIRES_NEW` inside a pessimistic-locked tx → deadlock risk. Avoid.
- Saga orchestrator methods must be `@Transactional` — saga state + outbox events in one tx.

## 4. Async Communication Pattern

| Use Case | Protocol |
|---|---|
| Need result immediately (validation, reservation) | gRPC blocking stub |
| Fire-and-forget, eventual consistency | Kafka via outbox |
| Cart data in order creation | REST (CartClient) |

## 5. Idempotency

Kafka consumers that trigger side effects (notifications, payments) MUST check for duplicate processing.

Pattern:
```java
if (processedEventRepository.existsByEventIdAndEventType(eventId, type)) return;
// ... do work ...
processedEventRepository.save(ProcessedEvent.of(eventId, type));
```

gRPC calls from saga use `idempotencyKey = orderNumber` for payment de-duplication.

## 6. Null Safety

- Return `Optional<T>` when absence is expected. Never return `null`.
- Use `.orElseThrow(() -> new ResourceNotFoundException(...))` not `.get()`.
- Validate inputs at method top with `Objects.requireNonNull(...)`.
- Collections: return `List.of()` or `Collections.emptyList()`, never `null`.

## 7. Error Handling

All REST errors → `ProblemDetail` (RFC 7807). `GlobalExceptionHandler` in common-lib handles:
- `ResourceNotFoundException` → 404
- `IllegalArgumentException` → 400
- `OptimisticLockingFailureException` → 409
- Unexpected → 500 (no stack trace exposed)

gRPC errors → correct gRPC status codes:
- `NOT_FOUND`, `INVALID_ARGUMENT`, `ALREADY_EXISTS`, `INTERNAL`

## 8. Security

- All endpoints (except health/actuator) require valid JWT via api-gateway.
- Internal gRPC: `usePlaintext()` in dev. TLS required in production.
- Never log sensitive fields: passwords, tokens, card numbers, full phone numbers.
- Secrets in `.env` only. Never hardcode in `application.yml` (use `${ENV_VAR:default}`).

## 9. Compensation in Saga

If any saga step fails, compensate in reverse order:
1. Refund payment (if `paymentId` set)
2. Release stock (if `reservationId` set)
3. Mark order FAILED
4. Publish `order.cancelled` via outbox

Compensation failures are logged but do not throw — saga must reach terminal state.

## 10. Virtual Thread Rules

- Do not use `synchronized` keyword in code that runs on virtual threads.
- Use `ReentrantLock` instead.
- HikariCP: safe with virtual threads out of the box.
- Kafka consumers: platform threads by default — leave as-is.
