---
type: ai-context
scope: project-wide
last-updated: 2026-04-27
---

# Current State

## Build Status
- Maven multi-module build: **functional**
- Fixed 2026-04-27: Vonage SDK removed from notification-service (JitPack can't resolve it)
- Fixed 2026-04-27: `OrderApplicationServiceTest` aligned to saga-based cart deletion (not eager)

## Service Implementation Status

| Service | Status | Notes |
|---|---|---|
| api-gateway | ✅ Implemented | Spring Cloud Gateway, JWT, Redis rate-limit |
| user-service | ✅ Implemented | gRPC `ValidateUser` + `GetUser`, PostgreSQL |
| product-service | ✅ Implemented | CQRS, MongoDB + Elasticsearch + Redis, GraphQL |
| inventory-service | ✅ Implemented | gRPC server, L1/L2 cache, pessimistic lock |
| cart-service | ✅ Implemented | Redis-based, distributed lock via Redis |
| order-service | ✅ Implemented | Saga orchestration, transactional outbox, analytics |
| payment-service | ✅ Implemented | gRPC server, transactional outbox, **simulated** |
| notification-service | ✅ Implemented | Kafka consumer, idempotency, email+SMS **simulated** |
| fraud-service | 🚧 Skeleton | KafkaStreams topology stub only, not integrated |

## Simulated / Not Production-Ready

| Component | Config | Reality |
|---|---|---|
| Email | `simulate-email=true` | Logs to console, no SMTP |
| SMS | `sms-provider=console` | Logs to console. TextBelt available but uses test key |
| Payment | Hardcoded success | `PaymentGrpcServiceImpl` always returns SUCCESS |
| Fraud detection | Skeleton only | `FraudDetectionTopology` not wired to production flow |

## Recent Changes (2026-04-27)

- `notification-service`: Vonage → TextBelt SMS migration
  - Added: `SmsProvider` interface, `ConsoleSmsProvider`, `TextBeltSmsProvider`
  - Updated: `SmsConfig`, `application.yml`, `docker-compose.yml`
- `order-service`: `OrderApplicationServiceTest` fixed — cart deletion verified NOT called during createOrder (happens in saga.completeOrder)
- `common-lib/GlobalExceptionHandler`: Updated
- `inventory-service/CacheConfig`: L1 Caffeine (5min) + L2 Redis (60min) composite

## Infrastructure
- Docker Compose: all infrastructure services configured with healthchecks
- Observability: OpenTelemetry → Jaeger (traces) + Prometheus/Grafana (metrics) + ELK (logs)
- MongoDB: runs as replica set (rs0) — required for Debezium CDC in product-service
- Pre-commit: `.githooks/pre-commit` + gitleaks secret scanning via `.gitleaks.toml`
