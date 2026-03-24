# Coding Standards — Java 21 / Spring Boot 3.x

## Language & Runtime
- Java 21 with Virtual Threads enabled (`spring.threads.virtual.enabled=true`). Never block virtual threads with `synchronized` blocks on I/O — use `ReentrantLock` or async patterns instead.
- Use Java 21 features where they simplify code: `record`, sealed classes, pattern matching in `switch`, text blocks.

## DTOs and Value Objects
- All DTOs **must** be Java `record` types — never plain classes with getters/setters.
- Domain value objects should be immutable. Prefer `record` or `final` class with constructor validation.

## Naming Conventions
- Classes: `PascalCase`. Methods/fields: `camelCase`. Constants: `UPPER_SNAKE_CASE`.
- Spring beans: name matches role — `UserService`, `OrderRepository`, `PaymentEventPublisher`.
- Kafka topics: `kebab-case` (e.g., `order-created`, `payment-failed`).
- gRPC services: match proto file name exactly.

## SOLID Principles
- Single Responsibility: one class, one reason to change. If a service grows beyond ~200 lines, split it.
- Open/Closed: use interfaces for extensibility — never modify stable domain logic directly.
- Dependency Inversion: always inject interfaces, not concrete implementations.

## Spring Boot Conventions
- Use constructor injection only — never `@Autowired` field injection.
- `@Service` for business logic, `@Repository` for data access, `@RestController` for HTTP.
- `@Transactional` must be placed on the service layer, not the repository layer.
- Never expose JPA entities directly from controllers — always map to DTOs.

## General
- No magic numbers or strings — define constants.
- Keep methods under 20 lines. Extract helpers when logic is complex.
- Delete dead code rather than commenting it out.
