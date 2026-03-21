# 🚀 Monat E-Commerce Platform - Quick Deploy

## One-Command Deployment

### Start Full Stack
```powershell
# Clone and navigate to project
cd c:\Users\Monat\Desktop\Projects\monat-ecommerce

# Start everything (Infrastructure + All 8 Services)
docker-compose up --build -d

# Watch logs
docker-compose logs -f

# Check status
docker-compose ps
```

### Access Points
| Service | URL | Credentials |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8080 | - |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | - |
| **Kafdrop** (Kafka UI) | http://localhost:9000 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger** | http://localhost:16686 | - |

## Test the System (5 Minutes)

### 1. Register User
```powershell
curl -X POST http://localhost:8080/api/users/register `
  -H "Content-Type: application/json" `
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "password123",
    "fullName": "John Doe"
  }'
```

### 2. Login
```powershell
curl -X POST http://localhost:8080/api/users/login `
  -H "Content-Type: application/json" `
  -d '{
    "username": "john",
    "password": "password123"
  }'

# Save the JWT token from response
```

### 3. Browse Products (Elasticsearch Search)
```powershell
# Search all products
curl http://localhost:8080/api/products

# Search with filters
curl "http://localhost:8080/api/products/search?keyword=laptop&minPrice=500&maxPrice=2000"
```

### 4. Add to Cart
```powershell
curl -X POST "http://localhost:8080/api/cart/session-abc123/items" `
  -H "Content-Type: application/json" `
  -d '{
    "productId": "PROD-001",
    "productName": "MacBook Pro 16-inch",
    "quantity": 1,
    "price": 1299.99
  }'
```

### 5. Create Order (Saga Orchestration!)
```powershell
curl -X POST http://localhost:8080/api/orders `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer YOUR_JWT_TOKEN" `
  -d '{
    "userId": 1,
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 1,
        "price": 1299.99
      }
    ],
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "paymentMethod": "CREDIT_CARD"
  }'
```

**What Just Happened:**
1. ✅ Order Service validated user (gRPC → User Service)
2. ✅ Reserved inventory (gRPC → Inventory Service)
3. ✅ Processed payment (gRPC → Payment Service)
4. ✅ Published Kafka events (`order.created`, `payment.completed`)
5. ✅ Notification Service sent confirmation email/SMS
6. ✅ Order status updated to CONFIRMED

### 6. Monitor the Flow

**Jaeger (Distributed Tracing):**
```
http://localhost:16686
Search: "order-service"
```

**Kafdrop (Kafka Topics):**
```
http://localhost:9000
Topics: order.created, payment.completed, payment.failed
```

**Grafana (Metrics):**
```
http://localhost:3000
Create dashboard with JVM metrics, HTTP requests, etc.
```

## Project Structure

```
monat-ecommerce/
├── api-gateway/         ← Port 8080 (Entry point)
├── user-service/        ← Port 8081, 9081 (gRPC)
├── product-service/     ← Port 8082 (MongoDB + Elasticsearch)
├── inventory-service/   ← Port 8083, 9083 (PostgreSQL + Redis)
├── cart-service/        ← Port 8084 (Redis)
├── order-service/       ← Port 8085 (Saga Orchestrator!)
├── payment-service/     ← Port 8086, 9086 (gRPC)
├── notification-service/← Port 8087 (Kafka Consumer)
├── common-lib/          ← Shared utilities
├── event-models/        ← Kafka events
├── grpc-proto/          ← gRPC definitions
├── docker-compose.yml   ← Full stack deployment
└── walkthrough.md       ← Complete documentation
```

## Tech Stack at a Glance

**Services:** Spring Boot 3.2 + Spring Cloud  
**Databases:** PostgreSQL, MongoDB, Redis, Elasticsearch  
**Messaging:** Apache Kafka  
**Communication:** REST, gRPC, Kafka Events  
**Containerization:** Docker Compose
**Monitoring:** Prometheus, Grafana, Jaeger  
**Architecture:** Microservices, Event-Driven, Saga Pattern  

## Key Features

✅ **8 Microservices** with clear responsibilities  
✅ **Saga Pattern** for distributed transactions  
✅ **Outbox Pattern** for reliable event publishing  
✅ **gRPC** for internal communication  
✅ **Elasticsearch** for product search  
✅ **Redis Caching** for performance  
✅ **Event-Driven** architecture with Kafka  
✅ **Docker Compose** full-stack deployment  
✅ **Observability** (tracing, metrics, monitoring)  

## Stop Services

```powershell
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

1. **Explore the Code:** Each service has clean DDD-style architecture
2. **Read Documentation:** Check `walkthrough.md` for detailed features
3. **Extend:** Add tests, CI/CD, service mesh, etc.

## Documentation

| File | Purpose |
|------|---------|
| `README.md` | Project overview |
| `QUICKSTART.md` | Getting started guide |
| `walkthrough.md` | Complete feature documentation |
| `DOCKER-COMPOSE-GUIDE.md` | Docker deployment details |
| `DOCKER-BUILD.md` | Docker build instructions |

## Troubleshooting

**Services not starting?**
```powershell
docker-compose logs <service-name>
docker-compose ps
```

**Port conflicts?**
```powershell
# Edit docker-compose.yml and change port mappings
# Example: "8081:8080" instead of "8080:8080"
```

**Clean restart:**
```powershell
docker-compose down -v
docker-compose up --build -d
```

---

**🎉 Your production-ready e-commerce platform is running!**

Built with ❤️ using modern microservices architecture.
