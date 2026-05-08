---
type: ai-context
scope: domain-spec
service: order-service
port-http: 8085
port-grpc: none
db: PostgreSQL (orderdb)
last-updated: 2026-04-27
---

# order-service Domain Spec

## Responsibility
Owns order lifecycle. Orchestrates distributed transaction via saga pattern. Produces Kafka events.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `OrderApplicationService` | `application/service/` | createOrder, getOrder, listOrders, analytics |
| `OrderSagaOrchestrator` | `domain/service/` | Saga steps: validateUser → reserveStock → processPayment → completeOrder |
| `Order` | `domain/model/` | Aggregate root |
| `OrderSagaState` | `domain/model/` | Tracks saga step + status for compensation |
| `OutboxEvent` | `domain/model/` | Outbox table entity |
| `OutboxEventPublisher` | `infrastructure/messaging/` | `@Scheduled` poller → publishes to Kafka |
| `CartClient` | `infrastructure/client/` | REST client to cart-service |
| `JdbcOrderAnalyticsRepository` | `infrastructure/reporting/` | Raw JDBC for analytics queries |

## Saga Steps (OrderSagaOrchestrator)

```
STARTED
  → USER_VALIDATED   (gRPC: user-service.ValidateUser)
  → STOCK_RESERVED   (gRPC: inventory-service.ReserveStock)
  → PAYMENT_PROCESSED (gRPC: payment-service.ProcessPayment)
  → ORDER_COMPLETED  (gRPC: inventory-service.CommitStock → mark order complete → publish event → delete cart)

On failure at any step:
  → COMPENSATING
    → STOCK_RELEASED  (if reservationId set)
    → PAYMENT_REFUNDED (if paymentId set)
  → COMPENSATION_COMPLETED
```

## Critical: Saga runs on raw Thread
`OrderApplicationService:112`: `new Thread(() -> saga.executeOrderSaga(...)).start()`  
Order returns PENDING immediately; saga runs async. Cart deleted only after saga completes successfully.

## Outbox Events Published

| Topic | Event Class | Trigger |
|---|---|---|
| `order.completed` | `OrderCompletedEvent` | Saga completeOrder step |
| `order.cancelled` | `OrderCancelledEvent` | Saga compensation |

**Missing**: `order.created` is NOT published (see remaining-issues.md #3).

## DB Tables
- `orders`, `order_items`, `shipping_address` (embedded)
- `order_saga_state` — saga tracking
- `outbox_events` — transactional outbox

## Analytics
`JdbcOrderAnalyticsRepository`: raw JDBC queries for:
- `findUserOrderHistory(userId, page, size)`
- `findOrders(status, page, size)`
- `findDailySalesReport(startDate, endDate)`
- `findOrderStatusDistribution()`

## REST Endpoints
```
POST   /api/v1/orders              createOrder
GET    /api/v1/orders/{id}         getOrderById
GET    /api/v1/orders/number/{n}   getOrderByNumber
GET    /api/v1/orders/user/{uid}   getUserOrders
GET    /api/v1/orders              listOrders (status filter)
GET    /api/v1/orders/analytics/daily-sales
GET    /api/v1/orders/analytics/status-distribution
```

## gRPC Clients Used
- `inventory-service` (`InventoryServiceBlockingStub`)
- `payment-service` (`PaymentServiceBlockingStub`)
- `user-service` (`UserServiceBlockingStub`)
