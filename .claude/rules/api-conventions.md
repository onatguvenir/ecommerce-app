# REST API Conventions

## URL Structure
- Use lowercase `kebab-case` for URL segments: `/api/v1/order-items`, not `/api/v1/orderItems`.
- Resource collections are plural nouns: `/products`, `/orders`, `/users`.
- Nested resources only one level deep: `/orders/{orderId}/items` — not `/orders/{orderId}/items/{itemId}/details`.
- Version in path: `/api/v1/...` always.

## HTTP Methods
- `GET`: read-only, no side effects, idempotent.
- `POST`: create a new resource or trigger an action.
- `PUT`: full replacement of a resource.
- `PATCH`: partial update.
- `DELETE`: removal, returns `204 No Content` on success.

## DTOs
- All request and response bodies must be Java `record` types.
- Never expose JPA entity fields directly.
- Use `@Valid` on `@RequestBody` parameters in controllers.
- Paginated responses must use `Page<T>` wrapper with `content`, `totalElements`, `totalPages`, `number`.

## Error Responses
Use Spring's `ProblemDetail` (RFC 7807) for all error responses:
```java
ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order not found");
problem.setProperty("orderId", orderId);
return ResponseEntity.of(problem).build();
```
Never return plain `String` error messages or custom error wrapper classes.

## HTTP Status Codes
- `200 OK`: successful GET, PUT, PATCH
- `201 Created`: successful POST creating a resource (include `Location` header)
- `204 No Content`: successful DELETE or action with no body
- `400 Bad Request`: validation failure
- `401 Unauthorized`: missing/invalid authentication
- `403 Forbidden`: authenticated but not authorized
- `404 Not Found`: resource does not exist
- `409 Conflict`: optimistic lock failure or duplicate
- `500 Internal Server Error`: unexpected — never expose stack traces
