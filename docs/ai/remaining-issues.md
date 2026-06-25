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

### ~~10. Cart deletion failure silently swallowed~~ ✅ FIXED 2026-06-09
**File**: `OrderSagaOrchestrator.deleteCartAfterCompletion()`
**Fix applied**: Cart removal extracted into a dedicated method that (a) retries up to
3 attempts on transient failure, (b) targets a new **idempotent** cart-service endpoint
`DELETE /api/cart/internal/{cartId}` (→ `CartApplicationService.deleteCart`, tolerates a
missing cart) instead of `DELETE /api/cart/{cartId}` which maps to the customer
`clearCart` (throws when the cart is absent), and (c) on final failure logs at ERROR and
increments `order_saga_step_total{step=delete_cart,result=failed}` instead of silently
swallowing — orphaned carts are now observable via metric/alert.
**Out of scope (still open)**: the *dominant* runtime cause is the saga aborting before
`completeOrder` (e.g. `ObjectOptimisticLockingFailureException` on `OrderSagaState`), which
correctly leaves the cart in place for a genuinely failed order — but the spurious
optimistic-lock failure itself is a separate saga-internal defect. See
`cart-not-deleted-root-cause.md` İhtimal 1b.

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

### ~~16. payment-service outbox events never published to Kafka~~ ✅ FIXED 2026-06-10
**Files**: `PaymentServiceApplication`, `common-lib/.../config/ShedLockConfig.java`
**Symptom**: `PaymentOutboxEventPublisher.@Scheduled publishPendingEvents()` never ran →
persisted `payment_outbox_events` rows stayed `processed=false` → `PaymentCompleted`/
`PaymentFailed` never reached Kafka (notification-service got no payment events). Saga itself
unaffected (gRPC-synchronous), so orders still COMPLETED — the gap was invisible until the
outbox `@Version`/manual-id bug (#10 / payment fix) was fixed and rows first started to persist.
**Two causes, both fixed**:
1. `PaymentServiceApplication` was missing `@EnableScheduling` (order-service & inventory-service
   have it) → poller never fired. Added `@EnableScheduling`.
2. After enabling, poller threw `NoSuchBeanDefinitionException: LockProvider`. common-lib
   `ShedLockConfig` declared the `LockProvider` bean as `@ConditionalOnBean(DataSource.class)`,
   but in a component-scanned user `@Configuration` that condition is evaluated **before**
   `DataSourceAutoConfiguration` runs. order-service passed the condition only because it defines
   an explicit early `DataSource` bean (`OrderDataSourceConfig`); payment-service relies on the
   auto-configured `DataSource`, so the condition was `false` and no `LockProvider` was created.
**Robust fix (applied to common-lib, hardens all services)**: rewrote `ShedLockConfig.lockProvider`
   to resolve the `DataSource` lazily via `ObjectProvider<DataSource>` instead of
   `@ConditionalOnBean`. The bean is now created deterministically at instantiation time (after
   `DataSourceAutoConfiguration`): services with a relational `DataSource` (order, payment,
   inventory) get the real `JdbcTemplateLockProvider`; Redis-only services (cart) get a harmless
   no-op provider they never exercise. Note: a pure `@AutoConfiguration(after =
   DataSourceAutoConfiguration.class)` was rejected because `com.monat.ecommerce.common` is
   component-scanned by every service, so an auto-config class there would be double-registered and
   still condition-checked at scan time. The earlier payment-local `SchedulerLockConfig` workaround
   was removed (the common bean now provides it; keeping both would duplicate the `lockProvider`
   bean name and fail startup).
**Verified e2e**: shedlock row `pollAndPublishPaymentEvents` active, both pending rows
`processed=t`, 2 messages on Kafka topic `payment.completed`, 0 scheduler errors.

### 15. api-gateway JWT config not confirmed
**Issue**: JWT secret/JWKS URL configuration for api-gateway not visible in current review. Verify `application.yml` of api-gateway has correct JWT validation setup.
