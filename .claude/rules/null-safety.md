# Null Safety Rules

## Guard Clauses (Fail Fast)
Validate inputs at the top of methods. Do not nest validation inside business logic.

```java
// CORRECT
public void processOrder(UUID orderId) {
    Objects.requireNonNull(orderId, "orderId must not be null");
    // ... business logic
}

// WRONG
public void processOrder(UUID orderId) {
    if (orderId != null) {
        // ... nested business logic
    }
}
```

## Optional Usage
- Use `Optional<T>` as a return type when a value may legitimately be absent.
- Never return `null` from a method — return `Optional.empty()` or throw a domain exception.
- Never call `.get()` on an Optional without a prior `.isPresent()` check — use `.orElseThrow()` or `.orElse()`.

```java
// CORRECT
public Optional<User> findById(UUID id) { ... }

// WRONG
public User findById(UUID id) { return null; }
```

## Validation Annotations
- All request body DTOs must be annotated and validated at the controller layer:
  - `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email` as appropriate
  - Controllers must have `@Valid` on `@RequestBody` parameters
- Domain entities must not rely solely on annotation validation — validate in constructors too.

## Spring Bean Dependencies
- All required bean dependencies must be `final` fields injected via constructor.
- Optional dependencies should be `@Autowired(required = false)` on a setter — never a nullable final field.

## Collections
- Never return `null` for collection return types — return `Collections.emptyList()` or `List.of()`.
