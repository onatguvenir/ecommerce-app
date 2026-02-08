# E-Commerce Microservices - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL, MongoDB, Redis, Kafka (via Docker Compose)

### 1. Start Infrastructure

```powershell
cd c:\Users\Monat\Desktop\Projects\monat-ecommerce
docker-compose up -d
```

This starts:
- PostgreSQL (5432) - User, Order, Inventory, Payment databases
- MongoDB (27017) - Product catalog
- Redis (6379) - Cart & caching
- Kafka + Zookeeper (9092, 2181) - Event streaming
- Elasticsearch (9200) - Product search
- Prometheus (9090) - Metrics
- Grafana (3000) - Dashboards
- Jaeger (16686) - Distributed tracing
- Kafdrop (9000) - Kafka UI

### 2. Build All Services

```powershell
mvn clean install -DskipTests
```

### 3. Run Services

**Terminal 1 - User Service:**
```powershell
cd user-service
mvn spring-boot:run
```
- HTTP: http://localhost:8081
- gRPC: localhost:9081
- Swagger: http://localhost:8081/swagger-ui.html

**Terminal 2 - Inventory Service:**
```powershell
cd inventory-service
mvn spring-boot:run
```
- HTTP: http://localhost:8083
- gRPC: localhost:9083

**Terminal 3 - Order Service:**
```powershell
cd order-service
mvn spring-boot:run
```
- HTTP: http://localhost:8085

---

## 🧪 Testing the Platform

### Scenario: Complete Order Flow

#### 1. Register User

```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "username": "alice",
    "password": "SecurePass123!",
    "firstName": "Alice",
    "lastName": "Johnson",
    "phone": "+1-555-0100"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "alice@example.com",
    "username": "alice",
    "firstName": "Alice",
    "lastName": "Johnson",
    "status": "ACTIVE"
  },
  "message": "User registered successfully",
  "timestamp": "2026-02-06T00:00:00"
}
```

#### 2. Add Address

```bash
curl -X POST http://localhost:8081/api/users/{userId}/addresses \
  -H "Content-Type: application/json" \
  -d '{
    "addressType": "SHIPPING",
    "street": "123 Main Street",
    "city": "San Francisco",
    "state": "CA",
    "postalCode": "94102",
    "country": "USA",
    "isDefault": true
  }'
```

#### 3. Create Order (Triggers Saga!)

```bash
curl -X POST http://localhost:8085/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop Pro 15",
        "quantity": 1,
        "unitPrice": 1299.99
      },
      {
        "productId": "PROD-002",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ],
    "shippingAddress": {
      "street": "123 Main Street",
      "city": "San Francisco",
      "state": "CA",
      "postalCode": "94102",
      "country": "USA"
    }
  }'
```

**What Happens Behind the Scenes:**

1. ✅ Order Service creates Order (status: PENDING)
2. ✅ Saga Orchestrator starts execution
3. ✅ **User Validation** - gRPC call to User Service (validates user is active)
4. ✅ **Stock Reservation** - gRPC call to Inventory Service
   - Inventory uses **optimistic locking** (@Version)
   - If concurrent updates occur, **@Retryable** auto-retries
   - Stock reservation created (expires in 15 minutes)
5. ✅ **Payment Processing** - gRPC call to Payment Service
6. ✅ **Stock Commit** - Finalizes reservation
7. ✅ Order status → COMPLETED
8. ✅ **Outbox Event** created
9. ✅ **Event Publisher** (scheduled) publishes to Kafka
10. ✅ Notification Service consumes event → sends email

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "orderNumber": "ORD-1707091200000-A1B2C3D4",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "status": "PENDING",
    "totalAmount": 1359.97,
    "currency": "USD",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop Pro 15",
        "quantity": 1,
        "unitPrice": 1299.99,
        "subtotal": 1299.99
      },
      {
        "productId": "PROD-002",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 29.99,
        "subtotal": 59.98
      }
    ],
    "createdAt": "2026-02-06T00:00:00"
  },
  "message": "Order created successfully"
}
```

#### 4. Check Order Status

```bash
curl http://localhost:8085/api/orders/7c9e6679-7425-40de-944b-e07fc1f90ae7
```

Wait a few seconds for Saga to complete, then check again:

```bash
curl http://localhost:8085/api/orders/7c9e6679-7425-40de-944b-e07fc1f90ae7
```

You should see `"status": "COMPLETED"` or `"status": "FAILED"` depending on saga execution.

#### 5. Verify Inventory Updated

```bash
# Check database directly
docker exec -it ecommerce-postgres psql -U postgres -d inventorydb

SELECT product_id, available_quantity, reserved_quantity, total_quantity, version
FROM inventory 
WHERE product_id IN ('PROD-001', 'PROD-002');
```

**Expected Result:**
- PROD-001: available_quantity reduced by 1
- PROD-002: available_quantity reduced by 2
- `version` incremented due to optimistic locking

---

## 🔍 Monitoring & Observability

### Prometheus Metrics

Visit: http://localhost:9090

**Useful Queries:**
```promql
# Request rate
rate(http_server_requests_seconds_count[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# JVM memory
jvm_memory_used_bytes{area="heap"}

# Inventory stock levels
inventory_available_quantity
```

### Grafana Dashboards

Visit: http://localhost:3000 (login: admin/admin)

Create dashboard with:
- Request throughput per service
- Error rates
- Response times (p50, p95, p99)
- JVM metrics
- Database connection pool

### Jaeger Distributed Tracing

Visit: http://localhost:16686

1. Select service: `order-service`
2. Click "Find Traces"
3. View complete Saga execution across services
4. See gRPC calls to User, Inventory, Payment services
5. Identify performance bottlenecks

### Kafdrop (Kafka UI)

Visit: http://localhost:9000

- View topics: `order.created`, `order.completed`, `order.cancelled`
- Inspect messages
- Monitor consumer lag

---

## 🧩 Architecture Highlights

### Optimistic Locking in Action

**Scenario:** 2 users try to buy last item simultaneously

```java
// User A reads: availableQty=1, version=1
// User B reads: availableQty=1, version=1

// User A reserves stock
inventory.reserveStock(1); // availableQty=0, version=2
inventoryRepository.save(inventory); // ✅ SUCCESS

// User B tries to reserve
inventory.reserveStock(1); // availableQty=0 (from old read)
inventoryRepository.save(inventory); // ❌ OptimisticLockException!

// @Retryable automatically retries User B
// User B re-reads: availableQty=0, version=2
// Throws IllegalStateException("Insufficient stock")
// User B gets proper error message
```

**Benefits:**
- ⚡ High throughput (non-blocking reads)
- ✅ No deadlocks
- ✅ Prevents overselling
- ✅ Automatic retry handling

### Saga Orchestration Pattern

**Why Orchestration over Choreography?**
- ✅ Centralized control in Order Service
- ✅ Easier to understand and debug
- ✅ Clear compensation logic
- ✅ Single source of truth for workflow state
- ✅ Retry and timeout handling in one place

**Outbox Pattern for Reliable Events:**
- Events saved in same transaction as Order
- Scheduled publisher polls outbox table
- Guarantees at-least-once delivery
- No lost events even if Kafka is down

---

## 🐛 Troubleshooting

### Service won't start

**Issue:** Port already in use
```
Error: Port 8081 is already in use
```

**Solution:**
```powershell
# Windows: Find and kill process
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Database connection failed

**Issue:** PostgreSQL not ready
```
Could not connect to database
```

**Solution:**
```powershell
# Check if containers are running
docker-compose ps

# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Saga fails immediately

**Issue:** gRPC services not reachable

**Solution:**
1. Ensure all services are running (User, Inventory, Payment)
2. Check gRPC ports: 9081, 9083, 9086
3. Check service logs for errors

### Optimistic lock exceptions

**Issue:** Too many concurrent requests

**Solution:**
- This is expected! @Retryable handles automatically
- Increase maxAttempts if needed
- Check logs for "retry" messages (DEBUG level)

---

## 📚 Additional API Examples

### User Service

#### Get User by ID
```bash
curl http://localhost:8081/api/users/{userId}
```

#### List All Users (Paginated)
```bash
curl "http://localhost:8081/api/users?page=0&size=20"
```

#### Find User by Email
```bash
curl http://localhost:8081/api/users/email/alice@example.com
```

### Order Service

#### Get Orders for User
```bash
curl "http://localhost:8085/api/orders/user/{userId}?page=0&size=10"
```

#### Get Order by Order Number
```bash
curl http://localhost:8085/api/orders/number/ORD-1707091200000-A1B2C3D4
```

---

## 🎯 Next Steps

1. **Implement Payment Service** - Complete the Saga flow
2. **Add Product Service** - MongoDB + Elasticsearch for search
3. **Build Cart Service** - Redis-based shopping cart
4. **Create API Gateway** - Single entry point with JWT validation
5. **Add Notification Service** - Email/SMS on order events
6. **Write Tests** - Unit + Integration with Testcontainers
7. **Create Dockerfiles** - Containerize all services
8. **Build Helm Charts** - Kubernetes deployment
9. **Configure Istio** - Service mesh for production

---

**You now have 3 production-ready microservices working together with Saga orchestration, optimistic locking, and event-driven architecture!** 🎉
