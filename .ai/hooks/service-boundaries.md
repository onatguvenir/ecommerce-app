---
type: ai-hook
trigger: when-crossing-service-boundaries
last-updated: 2026-04-27
---

# Service Boundary Reminders

When a task involves communication between services, use this as a reference.

## Allowed Cross-Service Communication

### gRPC (synchronous, blocking)
```
order-service → user-service      : ValidateUser (saga step 1)
order-service → inventory-service  : ReserveStock, CommitStock, ReleaseStock (saga steps 2,4)
order-service → payment-service    : ProcessPayment, RefundPayment (saga step 3, compensation)
notification-service → user-service: GetUser (fetch contact info for notifications)
```

### REST (internal, synchronous)
```
order-service → cart-service: GET  /api/v1/carts/{cartId}    (fetch cart items)
order-service → cart-service: DELETE /api/v1/carts/{cartId}  (cleanup after order complete)
```

### Kafka (asynchronous, via outbox)
```
order-service    → [order.created, order.completed, order.cancelled]   → notification-service
payment-service  → [payment.completed, payment.failed]                 → notification-service, fraud-service
```

## FORBIDDEN Cross-Service Communication

| Action | Why Forbidden |
|---|---|
| Service A queries Service B's PostgreSQL database | Breaks bounded context, creates coupling |
| Service A writes to Service B's database | Data integrity violation |
| Direct `kafkaTemplate.send()` in same transaction as DB write | Dual-write problem |
| Sharing JPA entity classes between services | Tight coupling |
| Circular gRPC calls (A calls B, B calls A) | Deadlock risk |

## When Adding New Cross-Service Call

### Adding gRPC call (new caller → existing server)
1. Verify proto method exists in `grpc-proto/src/main/proto/`
2. Add `@GrpcClient` stub injection in new caller
3. Add `grpc.client.<service-name>` config in caller's `application.yml`
4. See: `docs/ai/playbooks/add-grpc-method.md`

### Adding new gRPC method (new RPC)
1. Edit proto file in `grpc-proto` module
2. Regenerate stubs
3. Implement on server side
4. Wire client in caller
5. See: `docs/ai/playbooks/add-grpc-method.md`

### Adding new Kafka event flow
1. Define event in `event-models` module
2. Publisher saves to outbox (NOT kafkaTemplate.send directly)
3. Consumer checks idempotency + processes
4. See: `docs/ai/playbooks/add-kafka-event.md`

## Data Passed Across Boundaries

| Pass | Don't Pass |
|---|---|
| UUID references (userId, productId, orderId) | JPA entities |
| Primitive values (amounts, quantities, status strings) | Internal domain state |
| Event payloads (JSON-serializable records) | Mutable objects |
| Proto messages (between gRPC services) | Spring beans |

## Port Reference
For exact ports, see `docs/ai/architecture.md`.  
gRPC ports: user-service 9081, inventory-service 9083, payment-service 9086.
