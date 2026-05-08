---
type: ai-context
scope: domain-spec
service: payment-service
port-http: 8086
port-grpc: 9086
db: PostgreSQL (paymentdb)
last-updated: 2026-04-27
---

# payment-service Domain Spec

## Responsibility
Processes payments and refunds via gRPC. Publishes payment events via transactional outbox.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `PaymentGrpcServiceImpl` | `infrastructure/grpc/` | gRPC server: ProcessPayment, RefundPayment |
| `PaymentDomainService` | `domain/service/` | Core payment logic |
| `PaymentApplicationService` | `application/service/` | Application-layer coordination |
| `PaymentOutboxEventPublisher` | `infrastructure/messaging/` | `@Scheduled` poller → Kafka |
| `Payment` | `domain/model/` | Aggregate |
| `PaymentOutboxEvent` | `domain/model/` | Outbox entity |

## gRPC Server (port 9086)

### ProcessPayment
```protobuf
ProcessPaymentRequest {
  orderId, userId, amount, currency, paymentMethod, idempotencyKey
}
ProcessPaymentResponse {
  success, paymentId, paymentReference, message
}
```
**Currently**: Always returns `success=true`. No real gateway.

### RefundPayment
```protobuf
RefundPaymentRequest {
  paymentId, orderId, amount, reason
}
RefundPaymentResponse {
  success, refundId, message
}
```
**Currently**: Always returns `success=true`.

## Outbox Events Published

| Topic | Event Class | Trigger |
|---|---|---|
| `payment.completed` | `PaymentCompletedEvent` | Successful ProcessPayment |
| `payment.failed` | `PaymentFailedEvent` | Failed ProcessPayment |

## DB Tables
- `payments` — payment records
- `payment_outbox_events` — transactional outbox

## PaymentStatus Enum
`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`

## PaymentMethod Enum
`CARD`, `BANK_TRANSFER`, `CASH_ON_DELIVERY` (from proto: always "CARD" in current saga)

## Idempotency
Uses `idempotencyKey = orderNumber` from saga. Service must check for duplicate payment by idempotency key before processing.

## REST Endpoints
```
GET  /api/v1/payments/{id}         getPaymentById
GET  /api/v1/payments/order/{oid}  getPaymentByOrderId
```
(REST is secondary — primary interface is gRPC)
