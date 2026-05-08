---
type: ai-playbook
topic: debug-saga
last-updated: 2026-04-27
---

# Playbook: Debug Saga Failures

## Understand Saga State

### Check saga_state table
```sql
SELECT * FROM order_saga_state WHERE order_id = '<uuid>' ORDER BY updated_at DESC;
```
Columns: `order_id`, `current_step`, `status`, `reservation_id`, `payment_id`, `error_message`, `updated_at`

`status` values: `STARTED`, `COMPLETED`, `COMPENSATING`, `COMPENSATED`  
`current_step` values: `ORDER_CREATED` → `USER_VALIDATED` → `STOCK_RESERVED` → `PAYMENT_PROCESSED` → `ORDER_COMPLETED`  
Compensation steps: `STOCK_RELEASED` → `PAYMENT_REFUNDED` → `COMPENSATION_COMPLETED`

### Check outbox_events
```sql
SELECT * FROM outbox_events WHERE aggregate_id = '<order-uuid>' ORDER BY created_at;
```
If `processed_at` is NULL → event not published to Kafka yet (outbox poller may be down or slow).

## Common Failure Scenarios

### 1. USER_VALIDATED fails
**Symptom**: `status=COMPENSATING`, `current_step=COMPENSATION_COMPLETED`, `error_message` contains "User validation failed" or "User service unavailable"  
**Check**:
- Is user-service running? `docker compose logs user-service`
- Is the `userId` UUID valid and user exists?
- gRPC port 9081 reachable from order-service container?

### 2. STOCK_RESERVED fails
**Symptom**: stops at `ORDER_CREATED` (compensation with no reservationId set)  
**Check**:
- Is inventory-service running? `docker compose logs inventory-service`
- Does inventory exist for all product IDs in the order?
- `SELECT * FROM inventory_items WHERE product_id IN (...)`
- Is there enough stock? `quantity >= requested`
- gRPC port 9083 reachable?

### 3. PAYMENT_PROCESSED fails
**Symptom**: `current_step=STOCK_RELEASED`, `reservationId` set, `paymentId` null  
**Check**:
- Is payment-service running? `docker compose logs payment-service`
- gRPC port 9086 reachable?
- Is the `idempotencyKey` (orderNumber) already used by a previous failed attempt?

### 4. Saga stuck — never leaves STARTED
**Symptom**: order exists in DB, saga row exists, `status=STARTED` forever  
**Cause**: Saga runs on a separate thread (`new Thread(...).start()`). Thread may have crashed silently.  
**Check**: application logs for the order UUID around order creation time.  
**Fix**: Check `OrderApplicationService:112` thread exception handling.

### 5. Cart not deleted after successful order
**Symptom**: Order `status=COMPLETED`, cart still exists in Redis  
**Check**:
- `docker compose logs order-service | grep "Failed to delete cart"`
- Is cart-service running?
- Did cart TTL expire already? (Redis TTL may have removed it automatically)

## Log Commands
```bash
docker compose logs order-service --tail=100 --follow
docker compose logs inventory-service --tail=50
docker compose logs payment-service --tail=50
docker compose logs notification-service --tail=50
```

## Outbox Poller Health Check
If events in `outbox_events` have `processed_at = NULL` for > 5 seconds, the poller may be stuck.
```sql
SELECT COUNT(*) FROM outbox_events WHERE processed_at IS NULL;
```
Poller runs every 1000ms (`@Scheduled(fixedDelay = 1000)`). Restart the service if stalled.

## Trace Lookup
Find distributed trace for order: Jaeger UI at `http://localhost:16686`  
Service: `order-service`, Operation: `order.saga`  
Tags: `orderId=<uuid>`
