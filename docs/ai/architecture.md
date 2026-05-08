---
type: ai-context
scope: architecture
last-updated: 2026-04-27
---

# Architecture

## Stack
- **Java 21** + **Spring Boot 3.x** + Virtual Threads (`spring.threads.virtual.enabled=true`)
- Maven multi-module monorepo
- Docker Compose for local dev

## Infrastructure Ports

| Component | Port | Purpose |
|---|---|---|
| PostgreSQL | 5432 | user, order, inventory, payment, notification DBs |
| MongoDB | 27017 | product catalog (replica set rs0 required) |
| Redis | 6379 | cart storage, rate limiting, product/inventory cache |
| Kafka | 9092 | async event bus |
| AKHQ | 9000 | Kafka UI |
| RedisInsight | 8001 | Redis UI |
| Elasticsearch | 9200 | product full-text search |
| Logstash | 5044 | log ingestion |
| Kibana | 5601 | log UI |
| Prometheus | 9090 | metrics scraping |
| Grafana | 3000 | metrics dashboards |
| Jaeger | 16686 | distributed tracing UI |
| OTel Collector | 4317/4318 | telemetry pipeline (gRPC/HTTP) |
| Zookeeper | 2181 | Kafka coordination |

## Microservice Ports

| Service | HTTP | gRPC | DB | Cache | Kafka Role |
|---|---|---|---|---|---|
| api-gateway | 8080 | — | — | Redis | — |
| user-service | 8081 | 9081 | PostgreSQL | — | — |
| product-service | 8082 | — | MongoDB | Redis + ES | pub (domain events) |
| inventory-service | 8083 | 9083 | PostgreSQL | Caffeine + Redis | sub |
| cart-service | 8084 | — | Redis (primary store) | — | — |
| order-service | 8085 | — | PostgreSQL | — | pub via outbox |
| payment-service | 8086 | 9086 | PostgreSQL | — | pub via outbox |
| notification-service | 8087 | — | PostgreSQL | — | sub |
| fraud-service | 8088 | — | — | — | KafkaStreams |

## Communication Patterns

### Synchronous gRPC (blocking stubs)
```
order-service    → inventory-service : ReserveStock, CommitStock, ReleaseStock
order-service    → payment-service   : ProcessPayment, RefundPayment
order-service    → user-service      : ValidateUser
notification-service → user-service  : GetUser (with circuit breaker)
```

### Asynchronous Kafka (via Transactional Outbox)
```
order-service    publishes → order.created, order.completed, order.cancelled
payment-service  publishes → payment.completed, payment.failed
notification-service consumes ← order.created, order.completed, order.cancelled
notification-service consumes ← payment.completed, payment.failed
fraud-service    streams  ← payment events (KafkaStreams topology)
```

### REST (internal service calls)
```
order-service → cart-service : GET  /api/v1/carts/{cartId}
order-service → cart-service : DELETE /api/v1/carts/{cartId}
```

## Module Structure
```
monat-ecommerce/
├── common-lib/          # ApiResponse, PagedResponse, GlobalExceptionHandler, shared exceptions
├── event-models/        # Shared Kafka event POJOs (OrderCreatedEvent, PaymentCompletedEvent, etc.)
├── grpc-proto/          # .proto files → generated gRPC stubs (inventory, payment, user)
├── api-gateway/
├── user-service/
├── product-service/
├── inventory-service/
├── cart-service/
├── order-service/
├── payment-service/
├── notification-service/
└── fraud-service/
```

## DDD Package Structure (all services follow this)
```
com.monat.ecommerce.<service>/
├── application/
│   ├── dto/             # Java records (request/response)
│   ├── service/         # application-layer orchestration
│   └── mapper/
├── domain/
│   ├── model/           # JPA entities + domain aggregates + value objects
│   ├── repository/      # interfaces (port)
│   └── service/         # domain logic (saga, domain services)
└── infrastructure/
    ├── config/          # Spring @Configuration, metrics, resilience
    ├── controller/      # @RestController (HTTP)
    ├── grpc/            # gRPC clients (@GrpcClient) or server impls
    ├── messaging/       # Kafka @KafkaListener consumers, outbox publishers
    ├── persistence/
    │   ├── adapter/     # implements domain repository interface
    │   ├── entity/      # JPA @Entity classes
    │   ├── mapper/      # JPA entity ↔ domain model
    │   └── repository/  # JPA repositories
    └── bootstrap/       # @Component data seeders (dev only)
```

## Key Architectural Patterns

| Pattern | Where | Details |
|---|---|---|
| Saga Orchestration | order-service | `OrderSagaOrchestrator` drives gRPC calls; compensation on failure |
| Transactional Outbox | order-service, payment-service | DB write + outbox in same tx; `@Scheduled` poller publishes to Kafka |
| CQRS | product-service | Write: MongoDB via `ProductCommandService`; Read: Elasticsearch via `ProductQueryService` |
| Idempotent Consumer | notification-service | `ProcessedEvent` table deduplication per (eventId, eventType) |
| L1+L2 Cache | inventory-service | Caffeine (local, 5min) → Redis (distributed, 60min) via `CompositeCacheManager` |
| Distributed Lock | cart-service | Redis-based lock in `CartLockService` for concurrent cart mutations |
| Circuit Breaker | notification-service, order-service | Resilience4j on gRPC client calls |
| CDC (Debezium) | product-service | Debezium listens to MongoDB oplog → `DebeziumProductCacheListener` updates Redis |

## Proto Files
Located in `grpc-proto/src/main/proto/`:
- `inventory-service.proto` — `ReserveStock`, `CommitStock`, `ReleaseStock`
- `payment-service.proto` — `ProcessPayment`, `RefundPayment`
- `user-service.proto` — `ValidateUser`, `GetUser`
