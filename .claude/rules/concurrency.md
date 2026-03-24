# Concurrency & Locking Rules

## When to Use Pessimistic Write Lock
Apply `@Lock(LockModeType.PESSIMISTIC_WRITE)` in repository queries for:
- **Financial records**: payment amounts, account balances
- **Inventory stock**: any decrement/increment of `quantity`
- **Order status transitions**: preventing duplicate state changes

Example:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") UUID productId);
```

## When to Use Optimistic Lock
Apply `@Version` on entities where concurrent updates are possible but conflicts are rare:
- Product catalog updates
- User profile updates
- Non-financial order metadata

Example:
```java
@Version
private Long version;
```

Handle `OptimisticLockingFailureException` at the service layer — retry or return a 409 Conflict to the caller.

## Virtual Threads and Blocking
- Never use `synchronized` keyword in Virtual Thread contexts. Use `ReentrantLock` instead.
- Database connections from HikariCP are safe with Virtual Threads — no changes needed.
- Kafka consumers run on platform threads by default — leave them as-is unless explicitly tuned.

## Anti-Patterns to Avoid
- Do NOT rely on application-level locks (in-memory `synchronized`) for distributed safety — two service instances will bypass them.
- Do NOT use `@Transactional` with `REQUIRES_NEW` inside a pessimistic-locked transaction — it will open a second connection and deadlock risk increases.
