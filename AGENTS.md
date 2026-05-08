# Monat E-Commerce — AI Agent Guide

## Context
Event-driven microservices platform. Java 21, Spring Boot 3.2, JDK Virtual Threads. 9 services: api-gateway, user, product, inventory, cart, order, payment, notification, fraud.

## Before Starting Any Task

1. Read `.ai/hooks/pre-task.md`
2. Read the relevant `docs/ai/domain-specs/<service>.md`
3. Check `docs/ai/remaining-issues.md` for known bugs before touching related code
4. Cross-service work → read `docs/ai/business-boundaries.md`

## Build Commands

```bash
# Single service
mvn compile -pl order-service --also-make -q

# Single service tests
mvn test -pl order-service -q

# Full build (no tests)
mvn install -DskipTests -q

# Docker stack
docker compose up -d
```

## Port Map

| Service            | HTTP  | gRPC  |
|--------------------|-------|-------|
| api-gateway        | 8080  | —     |
| user-service       | 8081  | 9081  |
| product-service    | 8082  | —     |
| inventory-service  | 8083  | 9083  |
| cart-service       | 8084  | —     |
| order-service      | 8085  | —     |
| payment-service    | 8086  | 9086  |
| notification-service| 8087 | —     |

## Architecture Rules (Non-negotiable)

### Transactional Outbox — MANDATORY
Never call `KafkaTemplate.send()` directly from a `@Transactional` method that also writes to the DB. Always write to `outbox_events` table in the same transaction. A `@Scheduled` poller publishes to Kafka. See `docs/ai/examples/outbox-implementation.md`.

### Locking
- Inventory stock, payment amounts, order status transitions → `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Product/user metadata concurrent updates → `@Version` (optimistic)
- No `synchronized` keyword — virtual threads; use `ReentrantLock`

### DTOs
- All request/response types must be Java `record`
- Never expose JPA entities from controllers
- `@Valid` on all `@RequestBody` parameters

### Null Safety
- Packages annotated `@NullMarked` (JSpecify) treat all types as non-null by default
- Use `@Nullable` from `org.jspecify.annotations` for optional fields
- Return `Optional<T>` or throw — never return `null`
- Never call `.get()` on Optional without `.isPresent()` — use `.orElseThrow()`

### Spring
- Constructor injection only — no `@Autowired` field injection
- `@Transactional` on service layer, not repository
- No `@Transactional(REQUIRES_NEW)` inside a pessimistic-locked transaction

## Code Style

- Methods under 20 lines; extract helpers for complex logic
- No magic numbers — define constants
- No comments explaining WHAT the code does; only WHY (non-obvious constraints)
- Delete dead code; don't comment it out
- Kafka topics: `kebab-case` (e.g., `order.created`)
- Classes: `PascalCase`, methods/fields: `camelCase`, constants: `UPPER_SNAKE_CASE`

## Domain Boundaries

| Domain     | Owns                        | Never reads from              |
|------------|-----------------------------|-------------------------------|
| order      | Order, OrderItem, Saga      | Payment table directly        |
| payment    | Payment, PaymentEvent       | Order table directly          |
| inventory  | Inventory, StockReservation | Cart directly                 |
| cart       | CartItem (Redis)            | Inventory DB directly         |
| user       | User, UserRole              | Order/payment data            |
| notification| ProcessedEvent             | Business domain tables        |

## Active Simulations (Not Production-Ready)

- **Payment**: Always returns SUCCESS. No real gateway.
- **Email**: `simulate-email=true` → logs to console
- **SMS**: `sms-provider=console` → logs to console (TextBelt wired, needs real key)
- **Fraud**: KafkaStreams skeleton — detection non-functional

## Known Issues

See `docs/ai/remaining-issues.md`. Key items:
- Issues #1-#3 (saga threading, detached entity, missing order.created event) → **FIXED 2026-05-04**
- Issues #4-#15: payment simulation, fraud skeleton, gRPC test gaps, etc.

## Anti-Patterns to Avoid

- `new Thread(...)` → use `@Async` with configured executor
- Calling `KafkaTemplate` inside `@Transactional` service method
- Returning `null` from service methods
- JPA entity in REST response
- `synchronized` blocks (breaks virtual threads)
- `@Transactional` on repository layer
- Modifying unrelated code while fixing a bug

## Playbooks

- Add Kafka event → `docs/ai/playbooks/add-kafka-event.md`
- Add gRPC method → `docs/ai/playbooks/add-grpc-method.md`
- Debug saga → `docs/ai/playbooks/debug-saga.md`
- Add new service → `docs/ai/playbooks/add-service.md`
