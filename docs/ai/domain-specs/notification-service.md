---
type: ai-context
scope: domain-spec
service: notification-service
port-http: 8087
port-grpc: none (client only)
db: PostgreSQL (notificationdb)
last-updated: 2026-04-27
---

# notification-service Domain Spec

## Responsibility
Consumes Kafka events; sends email + SMS notifications to users. Fetches user contact info from user-service via gRPC.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `OrderEventConsumer` | `infrastructure/messaging/` | Kafka listener: order.created/completed/cancelled |
| `PaymentEventConsumer` | `infrastructure/messaging/` | Kafka listener: payment.completed/failed |
| `EmailService` | `domain/service/` | Email sending (simulated) |
| `SmsService` | `domain/service/` | SMS delegation to SmsProvider |
| `SmsProvider` | `domain/service/` | Interface: `send(phoneNumber, message)` |
| `ConsoleSmsProvider` | `infrastructure/sms/` | Logs SMS to console |
| `TextBeltSmsProvider` | `infrastructure/sms/` | HTTP POST to TextBelt API |
| `SmsConfig` | `infrastructure/config/` | Activates correct provider via `sms-provider` config |
| `UserServiceClient` | `infrastructure/grpc/` | gRPC client to user-service (with circuit breaker) |
| `ProcessedEvent` | `domain/model/` | Dedup table entity |
| `ProcessedEventRepository` | `infrastructure/persistence/` | Check + insert dedup records |

## Kafka Topics Consumed

| Topic | Handler | Action |
|---|---|---|
| `order.created` | `OrderEventConsumer` | Email order confirmation |
| `order.completed` | `OrderEventConsumer` | Email order completed |
| `order.cancelled` | `OrderEventConsumer` | Email order cancelled |
| `payment.completed` | `PaymentEventConsumer` | Email payment receipt |
| `payment.failed` | `PaymentEventConsumer` | Email payment failure notice |

**Warning**: `order.created` topic has no publisher currently (order-service doesn't publish it). See remaining-issues.md #3.

## Idempotency Pattern
```java
String eventKey = event.getOrderId().toString();
String eventType = "ORDER_CREATED";
if (processedEventRepository.existsByEventIdAndEventType(eventKey, eventType)) return;
// ... process ...
processedEventRepository.save(ProcessedEvent.of(eventKey, eventType));
```
Wrapped in `@Transactional` — DB check + notification + dedup insert atomically.

## SMS Provider Config
```yaml
application.notification:
  sms-provider: console          # options: console | textbelt
  textbelt:
    api-key: textbelt_test       # use real key for paid SMS
```
`SmsConfig` `@ConditionalOnProperty` creates correct bean.

## Email Config
```yaml
application.notification:
  simulate-email: true           # set false for real SMTP
  from-email: noreply@monat-ecommerce.com
```

## gRPC Client: user-service
- Fetches `User` (email, firstName, lastName, phoneNumber) before sending notification
- Circuit breaker + retry via Resilience4j
- If user not found → notification skipped (logged as warn)

## DB Tables
- `processed_events` — dedup store: `(event_id, event_type, processed_at)`
