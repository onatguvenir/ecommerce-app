# 🎉 Complete End-to-End Saga Flow Testing Guide

## Overview

You now have a **fully functional Saga orchestration** with all three critical services working together:

1. **User Service** - User validation
2. **Inventory Service** - Stock reservation (with optimistic locking)
3. **Payment Service** - Payment processing (with idempotency)
4. **Order Service** - Saga orchestrator

---

## 🚀 Complete Test Scenario

### 1. Start All Infrastructure

```powershell
cd c:\Users\Monat\Desktop\Projects\monat-ecommerce
docker-compose up -d
```

**Wait ~30 seconds for all services to be healthy**, then verify:
```powershell
docker-compose ps
```

### 2. Build All Services

```powershell
mvn clean install -DskipTests
```

### 3. Start All Microservices

Open **4 separate terminal windows**:

**Terminal 1 - User Service:**
```powershell
cd user-service
mvn spring-boot:run
```
✅ Wait for: "Started UserServiceApplication"

**Terminal 2 - Inventory Service:**
```powershell
cd inventory-service
mvn spring-boot:run
```
✅ Wait for: "Started InventoryServiceApplication"

**Terminal 3 - Payment Service:**
```powershell
cd payment-service
mvn spring-boot:run
```
✅ Wait for: "Started PaymentServiceApplication"

**Terminal 4 - Order Service:**
```powershell
cd order-service
mvn spring-boot:run
```
✅ Wait for: "Started OrderServiceApplication"

---

## 🧪 Test Case 1: Successful Order Flow

### Step 1: Register User

```bash
curl -X POST http://localhost:8081/api/users/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"bob@example.com\",\"username\":\"bob\",\"password\":\"SecurePass123\",\"firstName\":\"Bob\",\"lastName\":\"Smith\",\"phone\":\"+1-555-0200\"}"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "bob@example.com",
    "username": "bob",
    "status": "ACTIVE"
  },
  "message": "User registered successfully"
}
```

**Copy the `id` from the response** - you'll need it for creating orders.

### Step 2: Check Initial Inventory

```bash
# Check database
docker exec -it ecommerce-postgres psql -U postgres -d inventorydb -c "SELECT product_id, available_quantity, reserved_quantity FROM inventory WHERE product_id IN ('PROD-001', 'PROD-002');"
```

**Expected output:**
```
 product_id | available_quantity | reserved_quantity 
------------+--------------------+-------------------
 PROD-001   |                100 |                 0
 PROD-002   |                500 |                 0
```

### Step 3: Create Order (THIS TRIGGERS THE SAGA!)

**Replace `<USER_ID>` with the actual user ID from Step 1:**

```bash
curl -X POST http://localhost:8085/api/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":\"<USER_ID>\",\"items\":[{\"productId\":\"PROD-001\",\"productName\":\"Laptop Pro 15\",\"quantity\":2,\"unitPrice\":1299.99},{\"productId\":\"PROD-002\",\"productName\":\"Wireless Mouse\",\"quantity\":3,\"unitPrice\":29.99}],\"shippingAddress\":{\"street\":\"456 Oak Ave\",\"city\":\"New York\",\"state\":\"NY\",\"postalCode\":\"10001\",\"country\":\"USA\"}}"
```

**Expected Response (immediately):**
```json
{
  "success": true,
  "data": {
    "id": "7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d",
    "orderNumber": "ORD-1707177600000-A1B2C3D4",
    "userId": "<USER_ID>",
    "status": "PENDING",  ← Will change to COMPLETED or FAILED
    "totalAmount": 2689.95,
    "items": [...]
  },
  "message": "Order created successfully"
}
```

### Step 4: Watch the Saga Execute (in real-time!)

**Look at the Order Service terminal logs** - you'll see:

```
INFO - Starting Saga for order: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
INFO - Validating user: 550e8400-e29b-41d4-a716-446655440000
INFO - User validated successfully
INFO - Reserving stock for order: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
INFO - Stock reserved successfully
INFO - Processing payment for order: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
```

**Then EITHER:**

✅ **Success Path:**
```
INFO - Payment processed successfully
INFO - Order completed successfully: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
```

❌ **Failure Path (70% chance with default config):**
```
WARN - Payment failed: Insufficient funds
WARN - Starting compensation for order: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
INFO - Releasing stock for order: 7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
INFO - Stock released
INFO - Compensation completed
```

### Step 5: Check Order Status (after a few seconds)

```bash
curl http://localhost:8085/api/orders/7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d
```

**You'll see EITHER:**
- `"status": "COMPLETED"` + payment reference
- `"status": "FAILED"` + cancellation reason

### Step 6: Verify Stock Updated

```bash
docker exec -it ecommerce-postgres psql -U postgres -d inventorydb -c "SELECT product_id, available_quantity, reserved_quantity, version FROM inventory WHERE product_id IN ('PROD-001', 'PROD-002');"
```

**If payment succeeded:**
```
 product_id | available_quantity | reserved_quantity | version
------------+--------------------+-------------------+---------
 PROD-001   |                 98 |                 0 |       2
 PROD-002   |                497 |                 0 |       2
```
Notice: `version` incremented twice (reserve + commit)

**If payment failed (compensation):**
```
 product_id | available_quantity | reserved_quantity | version
------------+--------------------+-------------------+---------
 PROD-001   |                100 |                 0 |       2
 PROD-002   |                500 |                 0 |       2
```
Stock returned! `version` still incremented (reserve + release)

### Step 7: Check Kafka Events

Visit: http://localhost:9000 (Kafdrop)

**Topics to check:**
- `order.created` - Always present
- `order.completed` - If payment succeeded
- `order.cancelled` - If payment failed
- `payment.completed` - If payment succeeded
- `payment.failed` - If payment failed

---

## 🧪 Test Case 2: Insufficient Stock (Saga Compensation)

```bash
curl -X POST http://localhost:8085/api/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":\"<USER_ID>\",\"items\":[{\"productId\":\"PROD-001\",\"productName\":\"Laptop Pro 15\",\"quantity\":999,\"unitPrice\":1299.99}],\"shippingAddress\":{\"street\":\"456 Oak Ave\",\"city\":\"New York\",\"state\":\"NY\",\"postalCode\":\"10001\",\"country\":\"USA\"}}"
```

**Expected in Order Service logs:**
```
ERROR - Stock reservation failed: Insufficient stock
WARN - Starting compensation
INFO - Compensation completed (no stock to release)
```

**Order status:** `FAILED`
**Reason:** "Stock reservation failed: Insufficient stock for product PROD-001..."

---

## 🧪 Test Case 3: Idempotency Test

Create the same order **twice** with the same idempotency key:

```bash
# First request
curl -X POST http://localhost:8085/api/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":\"<USER_ID>\",\"items\":[{\"productId\":\"PROD-003\",\"productName\":\"USB-C Cable\",\"quantity\":5,\"unitPrice\":19.99}],\"shippingAddress\":{\"street\":\"123 Test St\",\"city\":\"LA\",\"state\":\"CA\",\"postalCode\":\"90001\",\"country\":\"USA\"}}"
```

**Then immediately send the same request again.**

**Result:** Payment Service will detect duplicate idempotency key and return the **same payment result** without charging twice!

Check Payment Service logs:
```
INFO - Payment already processed (idempotent) - Returning existing payment
```

---

## 📊 Monitoring the Saga

### 1. Database - Saga State

```bash
docker exec -it ecommerce-postgres psql -U postgres -d orderdb -c "SELECT order_id, current_step, status, reservation_id, payment_id FROM order_saga_state ORDER BY created_at DESC LIMIT 5;"
```

**Example output:**
```
                order_id              | current_step   | status    | reservation_id | payment_id
--------------------------------------+----------------+-----------+----------------+------------
7a9b8c6d-5e4f-3a2b-1c0d-9e8f7a6b5c4d | ORDER_COMPLETED| COMPLETED | abc123-def456  | pay-789xyz
```

### 2. Outbox Events

```bash
docker exec -it ecommerce-postgres psql -U postgres -d orderdb -c "SELECT event_type, aggregate_id, processed FROM outbox_events ORDER BY created_at DESC LIMIT 5;"
```

### 3. Prometheus Metrics

Visit: http://localhost:9090

**Query:**
```promql
# Total orders created
sum(increase(http_server_requests_seconds_count{uri="/api/orders",method="POST"}[5m]))

# Payment success rate
sum(rate(payment_completed_total[5m])) / sum(rate(payment_processed_total[5m]))
```

### 4. Jaeger Distributed Tracing

Visit: http://localhost:16686

- Select service: `order-service`
- Click "Find Traces"
- View complete Saga flow across all 4 services
- See gRPC calls timing

---

## 🎯 Success Criteria

✅ **Order created** with status PENDING
✅ **User validated** via gRPC to User Service
✅ **Stock reserved** via gRPC to Inventory Service (with optimistic locking)
✅ **Payment processed** via gRPC to Payment Service (with idempotency)
✅ **On Success:**
   - Stock committed
   - Order status → COMPLETED
   - Payment reference recorded
   - Events published to Kafka

✅ **On Failure:**
   - Stock released (if reserved)
   - Payment refunded (if charged)
   - Order status → FAILED/CANCELLED
   - Cancellation event published

---

## 🔧 Adjusting Payment Failure Rate

Want to test successful orders more often? Edit `payment-service/src/main/resources/application.yml`:

```yaml
application:
  payment:
    failure-rate: 0.10  # 10% failure instead of 30%
```

Restart Payment Service.

---

## 🎉 What You've Achieved

You now have:
- ✅ **Complete Saga Orchestration** with automatic compensation
- ✅ **Optimistic Locking** preventing overselling under load
- ✅ **Idempotency** preventing duplicate charges
- ✅ **Event-Driven Architecture** with Kafka
- ✅ **Outbox Pattern** for reliable event delivery
- ✅ **gRPC** for low-latency inter-service calls
- ✅ **Full Observability** (Prometheus, Grafana, Jaeger)
- ✅ **Production-Ready** error handling and retries

**This is a reference implementation for microservices best practices!** 🚀
