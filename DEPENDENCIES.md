# Dependencies Reference

Quick reference for all key dependencies used across Monat E-Commerce Platform (Spring Boot 3.2.2 / Java 21).

---

## Core Framework

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `spring-boot-starter-web` | 3.2.2 | All services | REST API, embedded Tomcat |
| `spring-boot-starter-data-jpa` | 3.2.2 | user, order, inventory, payment | ORM, JPA repositories |
| `spring-boot-starter-data-mongodb` | 3.2.2 | product-service | MongoDB repositories |
| `spring-boot-starter-data-redis` | 3.2.2 | api-gateway, cart, inventory | Redis caching, rate limiting |
| `spring-boot-starter-validation` | 3.2.2 | All services | Bean validation (`@NotNull`, etc.) |
| `spring-boot-starter-security` | 3.2.2 | All services | Spring Security filter chain |
| `spring-boot-starter-actuator` | 3.2.2 | All services | Health, metrics, liveness endpoints |
| `spring-cloud-gateway` | 2023.0.0 | api-gateway | Reactive API Gateway, routing, filters |
| `spring-cloud-openfeign` | 2023.0.0 | order-service | Declarative HTTP client for inter-service calls |

---

## Messaging

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `spring-kafka` | 3.1.x | order, payment, notification, fraud | Kafka producer/consumer |
| `kafka-streams` | 3.6.1 | fraud-service | Stateful stream processing |

---

## Database & Migration

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `postgresql` | 42.7.1 | user, order, inventory, payment | PostgreSQL JDBC driver |
| `flyway-core` | 10.7.2 | user, order, inventory, payment | DB schema versioning & migration |
| `flyway-database-postgresql` | 10.7.2 | Same as above | Flyway PostgreSQL dialect |
| `mongodb-driver-sync` | 4.11.1 | product-service | MongoDB sync driver |

---

## gRPC

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `grpc-server-spring-boot-starter` | 3.1.0.RELEASE | user, inventory, payment | gRPC server auto-configuration |
| `grpc-client-spring-boot-starter` | 3.1.0.RELEASE | order-service | gRPC client stubs |
| `grpc-stub`, `grpc-protobuf` | 1.61.0 | grpc-proto module | Core gRPC runtime |
| `protobuf-java` | 3.25.2 | grpc-proto module | Protocol Buffers serialization |

---

## Resilience

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `resilience4j-spring-boot3` | 2.2.0 | user, order, cart | Circuit breaker, retry, rate limiter, bulkhead |
| `spring-boot-starter-aop` | 3.2.2 | Services with Resilience4j | AOP proxy for Resilience4j annotations |

---

## Distributed Scheduling

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `shedlock-spring` | 5.12.0 | order, payment, inventory (via common-lib) | Distributed `@Scheduled` lock |
| `shedlock-provider-jdbc-template` | 5.12.0 | Same | PostgreSQL-backed lock store |

---

## Security & Auth

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `jjwt-api` | 0.12.5 | common-lib | JWT token creation/validation |
| `jjwt-impl` | 0.12.5 | common-lib | JWT runtime implementation |
| `jjwt-jackson` | 0.12.5 | common-lib | JWT JSON serialization |

---

## Observability (Metrics, Tracing, Logging)

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `micrometer-registry-otlp` | 1.12.2 | All services (via common-lib) | Metrics export via OpenTelemetry |
| `micrometer-tracing-bridge-otel` | 1.12.2 | All services | Micrometer → OpenTelemetry bridge |
| `opentelemetry-api` | 1.34.1 | All services | OpenTelemetry API |
| `opentelemetry-exporter-otlp` | 1.34.1 | All services | OTLP exporter to collector |
| `logstash-logback-encoder` | 7.4 | All services | Structured JSON logging for ELK |

---

## API Documentation

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `springdoc-openapi-starter-webmvc-ui` | 2.3.0 | All REST services | Swagger UI & OpenAPI 3 spec |
| `springdoc-openapi-starter-common` | 2.3.0 | common-lib | `OpenApiCustomizer` for global headers |
| `springdoc-openapi-starter-webflux-ui` | 2.3.0 | api-gateway | WebFlux-compatible Swagger |

---

## Mapping & Utilities

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `mapstruct` | 1.5.5.Final | user, order, inventory, payment | Compile-time type-safe object mapper |
| `lombok` | 1.18.32 | All services | Boilerplate reduction (`@Builder`, `@Slf4j`, etc.) |
| `jackson-databind` | 2.16.x | All services | JSON serialization |
| `jackson-datatype-jsr310` | 2.16.x | All services | `LocalDate`/`LocalDateTime` JSON support |
| `commons-lang3` | 3.14.0 | All services | String utilities |
| `graphql-java-extended-scalars` | 21.0 | product-service | GraphQL `BigDecimal`, `Date` scalar types |

---

## Testing

| Library | Version | Used In | Purpose |
|---|---|---|---|
| `spring-boot-starter-test` | 3.2.2 | All services | JUnit 5, Mockito, MockMvc |
| `spring-security-test` | 6.2.x | user-service | Security test helpers |
| `testcontainers` | 1.19.3 | Integration tests | Docker-based infra for integration tests |
| `testcontainers:postgresql` | 1.19.3 | user, order, inventory, payment | PostgreSQL test container |
| `testcontainers:mongodb` | 1.19.3 | product-service | MongoDB test container |
| `spring-boot-testcontainers` | 3.2.2 | Integration tests | Spring context with Testcontainers |
| `nplus1-hunter` | main-SNAPSHOT | user-service (dev) | N+1 query detection |

---

## Security Scanning (Build-Time)

| Library | Version | Trigger | Purpose |
|---|---|---|---|
| `dependency-check-maven` (OWASP) | 9.0.9 | `mvn verify` | CVE vulnerability scanning |
| `snyk-maven-plugin` | 2.0.0 | `mvn verify` | Snyk dependency audit |
| `ossindex-maven-plugin` | 3.2.0 | `mvn validate` | OSS Index vulnerability audit |

> [!TIP]
> All SCanning plugins are **skipped by default** (`snyk.skip=true`, `owasp.skip=true`). Enable selectively:
> `mvn verify -Dowasp.skip=false -Dsnyk.skip=false`

---

## Code Quality

| Library | Version | Purpose |
|---|---|---|
| `jacoco-maven-plugin` | 0.8.11 | Code coverage reports |
| `checkstyle` | (project `checkstyle.xml`) | Code style enforcement |
