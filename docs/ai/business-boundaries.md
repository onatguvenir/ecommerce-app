---
type: ai-context
scope: domain-ownership
last-updated: 2026-04-27
---

# Business Boundaries

Each service owns its data. No direct DB access across service boundaries.

## Bounded Contexts

### api-gateway (8080)
**Owns**: routing rules, JWT validation, rate limit config  
**Does NOT own**: user data, product data, business logic  
**Contracts**: routes all `/api/v1/*` to downstream services; validates JWT before forwarding

### user-service (8081 / gRPC 9081)
**Owns**: user accounts, authentication, user profiles  
**Data**: `users` table in PostgreSQL (userdb)  
**Exposes**:
- REST: user CRUD, auth endpoints
- gRPC: `ValidateUser(userId)` → `{isValid, isActive, message}`; `GetUser(userId)` → `User`
**Does NOT own**: orders, cart, products, payments

### product-service (8082)
**Owns**: product catalog, product search, product pricing  
**Data**: MongoDB (`productdb`) as write store; Elasticsearch for search; Redis for cache  
**Exposes**:
- REST: product CRUD (`/api/v1/products`)
- GraphQL: product queries
**Does NOT own**: inventory levels, cart, orders  
**Note**: Product existence ≠ inventory. Stock lives in inventory-service.

### inventory-service (8083 / gRPC 9083)
**Owns**: stock levels, stock reservations  
**Data**: `inventory_items`, `stock_reservations` tables in PostgreSQL (inventorydb)  
**Exposes**:
- REST: inventory queries
- gRPC: `ReserveStock`, `CommitStock`, `ReleaseStock`
**Does NOT own**: product metadata, order status  
**Concurrency**: Uses `PESSIMISTIC_WRITE` lock on all stock mutations. Never use optimistic lock for stock.

### cart-service (8084)
**Owns**: shopping cart state (ephemeral)  
**Data**: Redis (cart as primary store, not a cache)  
**Exposes**: REST: cart CRUD (`/api/v1/carts/{cartId}`)  
**Does NOT own**: product pricing (reads from product-service), stock levels  
**Lifecycle**: Cart deleted by order-service saga after successful order completion

### order-service (8085)
**Owns**: orders, order saga state, order analytics  
**Data**: `orders`, `order_items`, `order_saga_state`, `outbox_events` in PostgreSQL (orderdb)  
**Exposes**: REST: order CRUD + analytics endpoints  
**Orchestrates**: saga → user validation → stock reservation → payment → commit → cart delete  
**Does NOT own**: payment execution, stock levels, user profiles  
**Async**: publishes `order.created`, `order.completed`, `order.cancelled` via outbox

### payment-service (8086 / gRPC 9086)
**Owns**: payment records, payment outbox  
**Data**: `payments`, `payment_outbox_events` in PostgreSQL (paymentdb)  
**Exposes**:
- gRPC: `ProcessPayment`, `RefundPayment` (called by order-service saga)
**Does NOT own**: orders, user balances  
**Async**: publishes `payment.completed`, `payment.failed` via outbox  
**Warning**: Currently simulates all payments as SUCCESS. Real payment gateway not integrated.

### notification-service (8087)
**Owns**: notification delivery, processed event deduplication  
**Data**: `processed_events` in PostgreSQL (notificationdb)  
**Consumes**: `order.created`, `order.completed`, `order.cancelled`, `payment.completed`, `payment.failed`  
**Does NOT own**: order state, user state (fetches user data from user-service via gRPC on each event)  
**Idempotency**: Checks `processed_events` table before each notification. Skips duplicates.

### fraud-service (8088)
**Owns**: fraud detection rules, suspicious account tracking  
**Data**: none (stateless KafkaStreams, state store in Kafka)  
**Consumes**: payment events via KafkaStreams  
**Publishes**: `UserAccountSuspendedEvent` (defined in event-models)  
**Status**: Skeleton — topology logic not complete, not integrated with user-service suspension flow

## Cross-Service Dependency Map

```
api-gateway
    └─▶ all services (routes)

order-service
    ├─▶ user-service     (gRPC: ValidateUser)
    ├─▶ inventory-service (gRPC: ReserveStock, CommitStock, ReleaseStock)
    ├─▶ payment-service  (gRPC: ProcessPayment, RefundPayment)
    └─▶ cart-service     (REST: GET+DELETE cart)

notification-service
    ├─▶ user-service (gRPC: GetUser — fetch email/phone for notification)
    └─▶ Kafka topics (consume)

fraud-service
    └─▶ Kafka topics (consume via KafkaStreams)
```

## Data Ownership Rules
- **Never** query another service's database directly.
- **Never** duplicate another service's master data. Reference by ID only.
- Product price in cart = snapshot at time of add (stale price is acceptable until checkout).
- Order total = calculated from cart items at order creation time.
