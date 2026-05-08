---
type: ai-hook
trigger: before-editing-any-file
last-updated: 2026-04-27
---

# File Editing Rules

Rules to check before editing any file in this project.

## Package Layer Rules

### Editing `domain/model/` files
- Entities and value objects. No Spring annotations except `@Entity`, `@Version`, `@Column`.
- Constructors should validate: `Objects.requireNonNull(...)`.
- No references to infrastructure layer (no `@Repository`, no JPA queries here).

### Editing `domain/service/` files
- Pure domain logic. No `@Scheduled`, no Kafka, no HTTP clients.
- May call domain repositories (interfaces only).
- `OrderSagaOrchestrator`: the saga entry point. gRPC calls via `@GrpcClient` injected stubs.

### Editing `application/service/` files
- `@Transactional` is correct here.
- Save entity + save outbox event in SAME method.
- Never call `kafkaTemplate.send()` here.

### Editing `infrastructure/messaging/` files
- `@Scheduled` poller: reads outbox, calls `kafkaTemplate.send()`, marks processed.
- `@KafkaListener` consumer: always `@Transactional`, always check idempotency first.

### Editing `infrastructure/persistence/` files
- `entity/`: JPA `@Entity` classes. Use `@Version` for optimistic lock where appropriate.
- `repository/`: Spring Data JPA interfaces. Add `@Lock` for pessimistic queries.
- `adapter/`: Implements domain `repository/` interface. Maps entity ↔ domain model.

### Editing `infrastructure/grpc/` files
- Server (`GrpcServiceImpl`): `@GrpcService`, extend `...ImplBase`, use `responseObserver` pattern.
- Client: `@GrpcClient("service-name")` field injection for blocking stub.

### Editing `infrastructure/config/` files
- `@Configuration` + `@Bean` only.
- No business logic.

## Cross-Cutting Rules

### application.yml changes
- New env var → add to `.env.example` with documentation.
- New env var → add to `docker-compose.yml` environment section.
- Use `${ENV_VAR:default}` pattern — never hardcode secrets.

### DTO changes (application/dto/)
- All DTOs must be `record` types.
- Adding field = consider backwards compatibility if consumed by other services.

### Outbox table schema changes
- Add Flyway migration: `V{N}__<description>.sql`
- Never alter outbox table structure without migration.

### gRPC proto changes
- Edit `grpc-proto/src/main/proto/` only.
- Run `mvn compile -pl grpc-proto --also-make` after change.
- All services importing grpc-proto must be recompiled.

## Safety Checks Before Committing
1. Does `mvn compile -pl <service> --also-make -q` pass?
2. Do tests pass for changed service?
3. Did I add outbox if publishing Kafka events?
4. Did I add `@Lock(PESSIMISTIC_WRITE)` for stock/payment mutations?
5. Did I update `.env.example` if I added env vars?
