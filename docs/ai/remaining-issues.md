---
type: ai-context
scope: tech-debt-and-issues
last-updated: 2026-04-27
---

# Remaining Issues

## Critical / Architecture Gaps

### ~~1. Saga runs on raw Thread (order-service)~~ ✅ FIXED 2026-05-04
`@Async("sagaTaskExecutor")` on `executeOrderSaga`. `AsyncConfig` creates `ThreadPoolTaskExecutor` (core=4, max=10, queue=100, graceful shutdown 30s).

### ~~2. Saga thread uses shared @Transactional — detached entity risk~~ ✅ FIXED 2026-05-04
`executeOrderSaga` signature changed to `(UUID orderId, String cartId)`. Order reloaded via `findByIdWithItems` at start of async transaction.

### ~~3. `order.created` Kafka event not published~~ ✅ FIXED 2026-05-04
`OrderCreatedEvent` now published to outbox inside `createOrder` same transaction. `OutboxEventPublisher` routes `eventType=OrderCreated` → `order.created` topic.

### 4. Payment always simulates SUCCESS
**File**: `payment-service/PaymentGrpcServiceImpl`  
**Issue**: No real payment gateway. Always returns success. Refund also simulated.  
**Fix needed before production**: Integrate actual payment processor (Stripe, iyzico, etc.)

## Medium Issues

### 5. fraud-service skeleton incomplete
**Files**: `FraudDetectionTopology`, `FraudServiceApplication`  
**Issue**: KafkaStreams topology defined but logic is minimal stub. `UserAccountSuspendedEvent` exists in event-models but no service consumes it to actually suspend accounts.  
**Impact**: Fraud detection non-functional.

### 6. Email simulated — no real SMTP
**Config**: `application.notification.simulate-email=true`  
**Fix**: Set `simulate-email=false` and configure real SMTP or transactional email service (SendGrid, SES).

### 7. SMS simulated — TextBelt test key
**Config**: `sms-provider=console`, `textbelt.api-key=textbelt_test`  
**Fix**: Set `sms-provider=textbelt`, provide real `TEXTBELT_API_KEY`.

### 8. Internal Docker ports exposed on host
**File**: `docker-compose.yml`  
**Issue**: Kafka (9092), PostgreSQL (5432), MongoDB (27017), Redis (6379) all mapped to host ports. No network isolation.  
**Fix**: Move internal services to Docker internal network, expose only api-gateway (8080).

### 9. Missing gRPC server tests
**Services**: `user-service` (gRPC 9081), `inventory-service` (gRPC 9083), `payment-service` (gRPC 9086)  
**Issue**: No tests for gRPC server implementations — only unit tests for application services.  
**Fix**: Add `@SpringBootTest(webEnvironment=NONE)` + `GrpcInProcessChannelFactory` tests.

### 10. Cart deletion failure silently swallowed
**File**: `OrderSagaOrchestrator.completeOrder():258`  
**Issue**: `cartClient.deleteCart()` failure is caught and logged but ignored. Cart may not be deleted.  
**Fix**: Add retry logic or compensation tracking for cart deletion.

### 11. OrderNumber generation not collision-safe
**File**: `OrderApplicationService.generateOrderNumber()`  
**Issue**: `"ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8)` — `currentTimeMillis()` has millisecond resolution, UUID substring may collide under high load.  
**Fix**: Use full UUID or database sequence.

## Low Priority / Improvements

### 12. open-in-view disabled but JPA show-sql inconsistent
Some services have `show-sql: true` (dev convenience) — should be `false` everywhere in non-dev profiles.

### 13. Debezium CDC listener incomplete
**File**: `product-service/DebeziumProductCacheListener`  
**Issue**: Listens to MongoDB oplog changes. Verify that product-service Debezium config is wired correctly in Docker Compose (Debezium connector not visible in current docker-compose.yml).

### 14. No integration tests for outbox publisher
**Files**: `OutboxEventPublisher` (order), `PaymentOutboxEventPublisher` (payment)  
**Issue**: No integration test verifying outbox → Kafka publish pipeline end-to-end.

### 15. api-gateway JWT config not confirmed
**Issue**: JWT secret/JWKS URL configuration for api-gateway not visible in current review. Verify `application.yml` of api-gateway has correct JWT validation setup.
