# Docker Build and Run Guide

## Building Individual Services

### Build All Services
```powershell
# Build from project root
docker build -t monat/api-gateway:latest -f api-gateway/Dockerfile .
docker build -t monat/user-service:latest -f user-service/Dockerfile .
docker build -t monat/product-service:latest -f product-service/Dockerfile .
docker build -t monat/inventory-service:latest -f inventory-service/Dockerfile .
docker build -t monat/cart-service:latest -f cart-service/Dockerfile .
docker build -t monat/order-service:latest -f order-service/Dockerfile .
docker build -t monat/payment-service:latest -f payment-service/Dockerfile .
docker build -t monat/notification-service:latest -f notification-service/Dockerfile .
```

### Build Script (PowerShell)
```powershell
# Save as build-all.ps1
$services = @(
    "api-gateway",
    "user-service",
    "product-service",
    "inventory-service",
    "cart-service",
    "order-service",
    "payment-service",
    "notification-service"
)

foreach ($service in $services) {
    Write-Host "Building $service..." -ForegroundColor Green
    docker build -t "monat/${service}:latest" -f "${service}/Dockerfile" .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build $service" -ForegroundColor Red
        exit 1
    }
}

Write-Host "All services built successfully!" -ForegroundColor Green
```

## Running with Docker Compose

### Full Stack (Infrastructure + Services)
```yaml
# Coming soon: Updated docker-compose.yml with all services
```

### Run Infrastructure Only (Current)
```powershell
docker-compose up -d
```

This starts:
- PostgreSQL (5432)
- MongoDB (27017)
- Redis (6379)
- Elasticsearch (9200)
- Kafka + Zookeeper (9092)
- Prometheus (9090)
- Grafana (3000)
- Jaeger (16686)

### Run Services Manually
After infrastructure is up:
```powershell
docker run -d --name api-gateway -p 8080:8080 --network monat-network monat/api-gateway:latest
docker run -d --name user-service -p 8081:8081 -p 9081:9081 --network monat-network monat/user-service:latest
# ... and so on
```

## Multi-Stage Build Benefits

✅ **Smaller Images**: Only JRE in runtime, not Maven + JDK  
✅ **Security**: Non-root user execution  
✅ **Health Checks**: Built-in health monitoring  
✅ **Layer Caching**: Dependencies cached separately from source  
✅ **Production-Ready**: Optimized for deployment  

## Docker Compose Network

All services should connect to the same Docker network:
```powershell
docker network create monat-network
```

## Environment Variables

Each service accepts environment variables:
```powershell
docker run -e SPRING_PROFILES_ACTIVE=docker \
           -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/userdb \
           monat/user-service:latest
```

## Next Steps

1. Update docker-compose.yml to include all 8 services
2. Add service dependencies and health checks
3. Configure environment variables
4. Set up service discovery (optional)
5. Deploy to Kubernetes

## Image Sizes (Approximate)

- API Gateway: ~250MB
- User Service: ~250MB
- Product Service: ~250MB
- Inventory Service: ~250MB
- Cart Service: ~230MB
- Order Service: ~260MB
- Payment Service: ~250MB
- Notification Service: ~240MB

**Total: ~2GB for all services**
