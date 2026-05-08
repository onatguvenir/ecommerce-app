---
type: ai-playbook
topic: add-new-service
last-updated: 2026-04-27
---

# Playbook: Add a New Microservice

## Step 1 — Create Maven Module
```bash
mkdir <service-name>
```

Add to root `pom.xml`:
```xml
<module><service-name></module>
```

Create `<service-name>/pom.xml` (copy from existing service, update `artifactId` and dependencies).

## Step 2 — Application Class
```java
package com.monat.ecommerce.<service>;

@SpringBootApplication
public class <Service>Application {
    public static void main(String[] args) {
        SpringApplication.run(<Service>Application.class, args);
    }
}
```

## Step 3 — Package Structure
```
src/main/java/com/monat/ecommerce/<service>/
├── application/
│   ├── dto/
│   └── service/
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
└── infrastructure/
    ├── config/
    ├── controller/
    └── persistence/
```

## Step 4 — application.yml Minimum Config
```yaml
spring:
  application:
    name: <service-name>
  threads:
    virtual:
      enabled: true
  jpa:
    open-in-view: false    # MANDATORY

server:
  port: 80XX   # pick next available port (see architecture.md)

management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

## Step 5 — Create PostgreSQL Database
Add to `docker/init-databases.sql`:
```sql
CREATE DATABASE <service>db;
```

## Step 6 — Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -pl <service-name> --also-make -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/<service-name>/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

## Step 7 — Add to docker-compose.yml
```yaml
<service-name>:
  build:
    context: .
    dockerfile: <service-name>/Dockerfile
  container_name: monat-<service-name>
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/<service>db
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
  ports:
    - "80XX:80XX"
  depends_on:
    postgres:
      condition: service_healthy
    kafka:
      condition: service_started
  networks:
    - monat-network
  healthcheck:
    test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:80XX/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 40s
```

## Step 8 — Security Config
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

## Checklist
- [ ] Module added to root `pom.xml`
- [ ] Port assigned (no conflict — see architecture.md)
- [ ] `spring.jpa.open-in-view: false` in config
- [ ] Virtual threads enabled
- [ ] Database created in `init-databases.sql`
- [ ] Dockerfile follows multi-stage + non-root user pattern
- [ ] `docker-compose.yml` entry with healthcheck
- [ ] Security config: actuator permitted, JWT for rest
- [ ] Update `docs/ai/architecture.md` with new service port/role
- [ ] Update `AGENTS.md` port map
