# Monat E-Commerce Microservices Platform

Production-ready e-commerce platform built with microservices architecture, event-driven design, and cloud-native technologies.

## 🏗️ Architecture Overview

### Technology Stack
- **Language:** Java 17+
- **Framework:** Spring Boot 3.2, Spring Cloud 2023.0
- **Databases:** PostgreSQL 16, MongoDB 7, Redis 7, Elasticsearch 8
- **Messaging:** Apache Kafka 3.6
- **Communication:** REST (external), gRPC (internal)
- **Containerization:** Docker, Kubernetes, Helm
- **Service Mesh:** Istio
- **Observability:** ELK Stack, Prometheus, Grafana, OpenTelemetry
- **Security:** OAuth2/OIDC, JWT, Keycloak

### Microservices

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| API Gateway | 8080 | Redis | Entry point, routing, rate limiting |
| User Service | 8081 | PostgreSQL | User management, authentication |
| Product Service | 8082 | MongoDB, ES | Product catalog, search |
| Inventory Service | 8083 | PostgreSQL, Redis | Stock management, reservations |
| Cart Service | 8084 | Redis | Shopping cart |
| Order Service | 8085 | PostgreSQL | Order processing, Saga orchestration |
| Payment Service | 8086 | PostgreSQL | Payment processing |
| Notification Service | 8087 | - | Email/SMS notifications |

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- (Optional) Kubernetes cluster (Minikube/Kind)

### Local Development with Docker Compose

1. **Clone the repository:**
```bash
cd c:\Users\Monat\Desktop\Projects\monat-ecommerce
```

2. **Build all services:**
```bash
mvn clean package -DskipTests
```

3. **Start infrastructure (databases, Kafka, etc.):**
```bash
docker-compose up -d postgres mongodb redis elasticsearch kafka zookeeper
```

4. **Start services:**
```bash
# Start each service individually or use IDE
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
# ... etc
```

### Using Docker Compose (All-in-One)

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (port 5432)
- MongoDB (port 27017)
- Redis (port 6379)
- Elasticsearch (port 9200)
- Kafka & Zookeeper (9092, 2181)
- All microservices

## 📝 API Documentation

Each service exposes Swagger UI:
- User Service: http://localhost:8081/swagger-ui.html
- Product Service: http://localhost:8082/swagger-ui.html
- Order Service: http://localhost:8085/swagger-ui.html

## 🔐 Security

### JWT Authentication
Services use JWT for authentication. Obtain a token via User Service registration/login.

### Example Registration:
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "username": "johndoe",
    "password": "securePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

## 🎯 Key Features

### Event-Driven Architecture
- **Kafka Topics**: order.created, payment.completed, stock.reserved, etc.
- **Outbox Pattern**: Ensures reliable event publishing
- **Saga Pattern**: Distributed transaction coordination

### Resilience
- Circuit Breaker (Resilience4j)
- Retry mechanisms
- Bulkhead isolation
- Graceful degradation

### Observability
- **Metrics**: Prometheus endpoints on `/actuator/prometheus`
- **Health Checks**: `/actuator/health` (liveness & readiness probes)
- **Tracing**: OpenTelemetry integration
- **Logging**: Structured logging with correlation IDs

## 🧪 Testing

### Run Unit Tests:
```bash
mvn test
```

### Run Integration Tests:
```bash
mvn verify
```

Integration tests use Testcontainers for PostgreSQL, MongoDB, Redis, and Kafka.

## 📦 Deployment

### Docker Build
```bash
# Build Docker images for all services
docker build -t monat-ecommerce/user-service:latest ./user-service
docker build -t monat-ecommerce/product-service:latest ./product-service
# ... etc
```

### Kubernetes Deployment
```bash
# Apply Kubernetes manifests
kubectl apply -f kubernetes/

# Or use Helm
helm install user-service ./helm-charts/user-service-chart
```

### Istio Service Mesh
```bash
# Label namespace for Istio injection
kubectl label namespace default istio-injection=enabled

# Apply Istio configurations
kubectl apply -f kubernetes/istio/
```

## 🔄 Saga Flow Example

### Successful Order Flow:
1. **Order Service**: Create order → Publish `OrderCreated`
2. **Inventory Service**: Reserve stock via gRPC → Publish `StockReserved`
3. **Payment Service**: Process payment via gRPC → Publish `PaymentCompleted`
4. **Order Service**: Mark order complete → Publish `OrderCompleted`
5. **Notification Service**: Send confirmation email

### Failed Order Flow (Compensation):
1. **Order Service**: Create order → Publish `OrderCreated`
2. **Inventory Service**: Reserve stock → Publish `StockReserved`
3. **Payment Service**: Payment fails → Publish `PaymentFailed`
4. **Inventory Service**: Release stock → Publish `StockRollback`
5. **Order Service**: Cancel order → Publish `OrderCancelled`
6. **Notification Service**: Send cancellation notice

## 📊 Monitoring

### Prometheus Metrics
Access metrics at: `http://localhost:9090`

### Grafana Dashboards
Access dashboards at: `http://localhost:3000`

Default credentials: admin/admin

## 🛠️ Development

### Project Structure
```
monat-ecommerce/
├── common-lib/              # Shared utilities, DTOs, exceptions
├── event-models/            # Kafka event schemas
├── grpc-proto/              # gRPC protocol definitions
├── api-gateway/             # API Gateway service
├── user-service/            # User management
├── product-service/         # Product catalog
├── inventory-service/       # Stock management
├── cart-service/            # Shopping cart
├── order-service/           # Order processing & Saga
├── payment-service/         # Payment processing
├── notification-service/    # Notifications
├── docker-compose.yml       # Local development setup
├── kubernetes/              # K8s manifests
└── helm-charts/             # Helm charts for deployment
```

### Adding a New Service
1. Create module under parent POM
2. Add dependencies (common-lib, event-models, grpc-proto)
3. Implement domain layer (entities, repositories)
4. Implement application layer (services, DTOs, mappers)
5. Implement infrastructure layer (controllers, gRPC, config)
6. Add database migrations (Flyway/Liquibase)
7. Configure application.yml
8. Add Dockerfile
9. Create Helm chart
10. Write tests

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📧 Contact

For questions or support, contact the development team.

---

**Built with ❤️ using Spring Boot and Cloud-Native technologies**
