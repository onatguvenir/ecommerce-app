Scaffold a new Spring Boot microservice following monat-ecommerce project conventions.

Service name (from $ARGUMENTS): use this as the module directory name and Maven artifactId.

Steps to perform:
1. Create the Maven module directory `$ARGUMENTS/` with a `pom.xml` inheriting from the root `pom.xml`
2. Create the standard package structure under `src/main/java/com/monat/ecommerce/$ARGUMENTS/`:
   - `domain/` — entities, value objects, domain events, repository interfaces
   - `application/` — service classes, command/query handlers
   - `infrastructure/` — controllers, JPA repositories, Kafka producers/consumers, config classes
3. Create `src/main/resources/application.yml` with:
   - `spring.application.name`, `server.port` (ask user for port — must not conflict with existing ports in CLAUDE.md)
   - `spring.threads.virtual.enabled: true`
   - Actuator health endpoints enabled
4. Create a main application class with `@SpringBootApplication`
5. Create `src/test/` mirror structure with a placeholder integration test class

Ask the user for: service port (HTTP), gRPC port if needed (or none), primary database (PostgreSQL/MongoDB/Redis/none).

After scaffolding, remind the user to add the new module to the root `pom.xml` modules list and update the port map in `CLAUDE.md`.
