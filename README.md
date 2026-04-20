# Monat E-Commerce Platform

Production-grade microservices e-commerce platform built with Spring Boot 3.x, Java 21, and event-driven architecture.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 + Virtual Threads |
| Framework | Spring Boot 3.x |
| API | REST (JSON), GraphQL (product-service), gRPC (internal) |
| Auth | JWT (HMAC-SHA256), validated at api-gateway |
| Messaging | Apache Kafka (KRaft mode), Transactional Outbox Pattern |
| Databases | PostgreSQL 16, MongoDB 7, Redis 7, Elasticsearch 8 |
| Observability | ELK Stack, Prometheus + Grafana, OpenTelemetry + Jaeger, SkyWalking APM |
| Build | Maven (multi-module) |
| Container | Docker + Docker Compose |
| Migrations | Flyway (PostgreSQL services) |

## Services

| Service | HTTP | gRPC | Storage |
|---------|------|------|---------|
| api-gateway | 8080 | — | Redis |
| user-service | 8081 | 9081 | PostgreSQL |
| product-service | 8082 | — | MongoDB + Elasticsearch + Redis |
| inventory-service | 8083 | 9083 | PostgreSQL + Redis |
| cart-service | 8084 | — | Redis |
| order-service | 8085 | — | PostgreSQL + Kafka |
| payment-service | 8086 | 9086 | PostgreSQL + Kafka |
| notification-service | 8087 | — | Kafka |
| fraud-service | 8098 | — | Kafka Streams |

## Infrastructure Ports

| Service | Port |
|---------|------|
| PostgreSQL | 5432 |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka | 9092 |
| AKHQ (Kafka UI) | 9000 |
| RedisInsight | 8001 |
| Elasticsearch | 9200 |
| Kibana | 5601 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Jaeger UI | 16686 |
| SkyWalking UI | 8088 |
| MailDev UI | 1080 |
| SonarQube | 9005 |

## Quick Start

```bash
# Start all services
docker compose up -d

# Check service health
docker compose ps

# View logs for a specific service
docker compose logs -f order-service

# Rebuild a specific service after code changes
docker compose build inventory-service && docker compose up -d inventory-service
```

## API Access

- **Swagger UI**: http://localhost:8080/swagger-ui.html (aggregated)
- **GraphQL**: http://localhost:8080/graphql (product queries)
- **GraphiQL**: http://localhost:8082/graphiql (product-service direct)

## Authentication

```bash
# Register
POST http://localhost:8080/api/users/register

# Login — returns JWT token
POST http://localhost:8080/api/users/login

# Use token in subsequent requests
Authorization: Bearer {token}
```

Routes requiring auth: `/api/orders/**`, `/api/payments/**`, `/api/inventory/**`

## Architectural Patterns

- **Saga Orchestration**: order-service orchestrates distributed transactions across user, inventory, and payment services via gRPC
- **Transactional Outbox**: order-service and payment-service publish Kafka events via outbox table to prevent dual-write issues
- **CQRS**: product-service separates command and query handlers; order-service uses read model with materialized views
- **Optimistic Locking**: `@Version` on inventory and order entities prevents overselling under concurrent load
- **Circuit Breaker / Retry**: Resilience4j wired in user-service, order-service, and cart-service for gRPC calls
