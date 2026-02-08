# Complete Docker Compose Deployment

## Quick Start - Full Stack

### 1. Start Everything (Infrastructure + All Services)
```powershell
# Build and start all services
docker-compose up --build -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f order-service
```

### 2. Check Service Health
```powershell
# Check all services
docker-compose ps

# All services should show "healthy" status
```

### 3. Access Services

| Service | URL | Description |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8080 | Main entry point |
| User Service | http://localhost:8081 | Direct access (dev only) |
| Product Service | http://localhost:8082 | Direct access (dev only) |
| Inventory Service | http://localhost:8083 | Direct access (dev only) |
| Cart Service | http://localhost:8084 | Direct access (dev only) |
| Order Service | http://localhost:8085 | Direct access (dev only) |
| Payment Service | http://localhost:8086 | Direct access (dev only) |
| Notification Service | http://localhost:8087 | Direct access (dev only) |
| **Kafdrop** (Kafka UI) | http://localhost:9000 | Monitor Kafka topics |
| **Grafana** | http://localhost:3000 | Metrics dashboards (admin/admin) |
| **Prometheus** | http://localhost:9090 | Metrics storage |
| **Jaeger** | http://localhost:16686 | Distributed tracing |

### 4. Test the Platform

```powershell
# Via API Gateway (recommended)
curl http://localhost:8080/api/products
curl http://localhost:8080/api/cart/session-123

# Create an order (triggers Saga!)
curl -X POST http://localhost:8080/api/orders `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <your-jwt>" `
  -d '{...}'
```

## Architecture

```
           ┌─────────────┐
           │ API Gateway │ :8080
           └──────┬──────┘
                  │
      ┌──────────┼──────────┐
      │          │          │
┌─────▼────┐ ┌──▼───┐ ┌───▼────┐
│  User    │ │ Cart │ │Product │
│  :8081   │ │ :8084│ │ :8082  │
│  :9081   │ │      │ │        │
└──────────┘ └──────┘ └────────┘
                  │
            ┌─────▼──────┐
            │   Order    │ :8085
            │   (Saga)   │
            └─────┬──────┘
                  │
         ┌────────┼────────┐
         │                 │
    ┌────▼─────┐     ┌────▼────┐
    │Inventory │     │ Payment │
    │  :8083   │     │  :8086  │
    │  :9083   │     │  :9086  │
    └──────────┘     └────┬────┘
                           │
                     ┌─────▼──────┐
                     │   Kafka    │
                     └─────┬──────┘
                           │
                   ┌───────▼────────┐
                   │ Notification   │
                   │     :8087      │
                   └────────────────┘
```

## Service Dependencies

```yaml
Infrastructure Layer:
  - PostgreSQL (User, Order, Inventory, Payment)
  - MongoDB (Product)
  - Redis (Inventory cache, Cart storage, API Gateway rate limiting)
  - Elasticsearch (Product search)
  - Kafka (Event streaming)

Service Layer (start order):
  1. API Gateway (depends on: Redis)
  2. User Service (depends on: PostgreSQL)
  3. Product Service (depends on: MongoDB, Elasticsearch)
  4. Inventory Service (depends on: PostgreSQL, Redis)
  5. Cart Service (depends on: Redis)
  6. Payment Service (depends on: PostgreSQL, Kafka)
  7. Order Service (depends on: PostgreSQL, Kafka, User, Inventory, Payment)
  8. Notification Service (depends on: Kafka)
```

## Health Checks

All services have health checks:
- **Liveness**: Service is running
- **Readiness**: Service is ready to accept traffic
- **Start period**: Extra time for JVM startup

```powershell
# Check health via actuator
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
# ... etc
```

## Scaling Services

Scale specific services:
```powershell
# Scale order service to 3 instances
docker-compose up -d --scale order-service=3

# Scale inventory service (high load)
docker-compose up -d --scale inventory-service=5
```

## Stopping and Cleanup

```powershell
# Stop all services
docker-compose down

# Stop and remove volumes (CAUTION: deletes data!)
docker-compose down -v

# Stop specific service
docker-compose stop order-service

# Restart specific service
docker-compose restart order-service
```

## Monitoring

### 1. Prometheus Metrics
- URL: http://localhost:9090
- Query examples:
  - `http_server_requests_seconds_count`
  - `jvm_memory_used_bytes`
  - `kafka_consumer_lag`

### 2. Grafana Dashboards
- URL: http://localhost:3000
- Login: admin/admin
- Import dashboards for Spring Boot metrics

### 3. Jaeger Tracing
- URL: http://localhost:16686
- Trace requests across services
- View Saga execution flow

### 4. Kafka UI (Kafdrop)
- URL: http://localhost:9000
- View topics: `order.created`, `payment.completed`, etc.
- Monitor consumer lag

## Troubleshooting

### Services not starting?
```powershell
# Check logs
docker-compose logs <service-name>

# Check container status
docker-compose ps

# Restart specific service
docker-compose restart <service-name>
```

### Database connection issues?
```powershell
# Check PostgreSQL logs
docker-compose logs postgres

# Verify databases created
docker exec -it monat-postgres psql -U postgres -c "\l"
```

### Kafka not working?
```powershell
# Check Kafka status
docker-compose logs kafka

# List topics
docker exec -it monat-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Development Workflow

1. **Make code changes**
2. **Rebuild specific service:**
   ```powershell
   docker-compose up -d --build order-service
   ```
3. **Watch logs:**
   ```powershell
   docker-compose logs -f order-service
   ```

## Production Notes

⚠️ This docker-compose is for **development/testing only**

For production:
- Use Kubernetes (see k8s/ directory)
- Separate infrastructure from services
- Use secrets management (not environment variables)
- Implement proper service mesh (Istio)
- Set up proper monitoring and alerting
- Use managed databases (AWS RDS, etc.)
- Implement backup and disaster recovery

## Resource Requirements

Minimum system resources:
- **CPU**: 4 cores
- **RAM**: 8GB
- **Disk**: 20GB

Recommended:
- **CPU**: 8 cores
- **RAM**: 16GB
- **Disk**: 50GB SSD
