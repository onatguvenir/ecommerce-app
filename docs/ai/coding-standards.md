---
type: ai-context
scope: coding-standards
last-updated: 2026-04-27
---

# Coding Standards

## Java 21 Features — Use These

| Feature | Usage |
|---|---|
| `record` | All DTOs (request/response). Never plain class with getters. |
| `sealed class` | Where polymorphism is closed and enumerable |
| Pattern matching `switch` | Replace instanceof chains |
| Text blocks `"""` | Multi-line SQL, JSON templates |
| Virtual Threads | Enabled globally via `spring.threads.virtual.enabled=true` |

## DTOs

```java
// CORRECT
public record CreateOrderRequest(
    @NotNull UUID userId,
    @NotBlank String cartId,
    List<@Valid OrderItemRequest> items
) {}

// WRONG — do not use
public class CreateOrderRequest {
    private UUID userId;
    // getters/setters...
}
```

All request records: `@Valid` on controller `@RequestBody`.  
All response records: never expose JPA entity fields directly.

## Spring Boot Conventions

| Rule | Detail |
|---|---|
| Injection | Constructor-only. Never `@Autowired` field injection. |
| `@Transactional` | Service layer only. Never controller or repository. |
| `@Service` | Business logic |
| `@Repository` | Data access |
| `@RestController` | HTTP endpoints |
| Entity exposure | Always map JPA entity → DTO before returning from service |

## Naming

| Context | Convention | Example |
|---|---|---|
| Classes | PascalCase | `OrderApplicationService` |
| Methods / fields | camelCase | `createOrder`, `totalAmount` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS` |
| Kafka topics | kebab-case | `order-created`, `payment-failed` |
| REST URLs | lowercase kebab-case | `/api/v1/order-items` |
| gRPC services | match proto service name | `InventoryService` |
| gRPC RPC methods | PascalCase verb+noun | `ReserveStock`, `GetUserById` |

## REST API

- Collections: plural nouns (`/products`, `/orders`)
- Versioned: always `/api/v1/...`
- Nested max 1 level: `/orders/{id}/items` ✓, `/orders/{id}/items/{iid}/details` ✗
- Errors: `ProblemDetail` (RFC 7807) — never plain String

HTTP status codes:
- `200` GET/PUT/PATCH success
- `201` POST create (include `Location` header)
- `204` DELETE success
- `400` validation failure
- `401` missing/invalid JWT
- `403` authenticated, not authorized
- `404` resource not found
- `409` optimistic lock conflict or duplicate
- `500` unexpected — no stack trace

## Method Size

Max 20 lines per method. Extract private helpers if longer.  
Max ~200 lines per class. Split responsibilities if larger.

## Code Cleanliness

- No magic strings/numbers — define constants
- Delete unused code, do not comment out
- No speculative features — YAGNI
- Comments only for non-obvious WHY, never for WHAT
- Match existing style in file being edited

## Package Naming
```
com.monat.ecommerce.<service>.<layer>.<sublayer>
```
Examples:
- `com.monat.ecommerce.order.application.service.OrderApplicationService`
- `com.monat.ecommerce.order.domain.model.Order`
- `com.monat.ecommerce.order.infrastructure.persistence.entity.OrderEntity`

## Validation

```java
// Domain layer
Objects.requireNonNull(orderId, "orderId must not be null");

// Controller layer
@PostMapping
public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {}

// Entity
@NotNull @Column(nullable = false)
private UUID userId;
```

## Common-lib Usage

From `common-lib`:
- `ApiResponse<T>` — standard single-item response wrapper
- `PagedResponse<T>` — paginated list response
- `ResourceNotFoundException` — 404 scenarios
- `GlobalExceptionHandler` — do not duplicate exception handling in services
