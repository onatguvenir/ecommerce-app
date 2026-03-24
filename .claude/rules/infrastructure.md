# Infrastructure Standards — Docker & Kubernetes

Cross-references: `DOCKER-COMPOSE-GUIDE.md` for compose usage, `DEPLOY-NOW.md` for deployment steps.

---

## Dockerfile Standards

### Multi-Stage Build (mandatory)
Every service Dockerfile must use at least two stages:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -pl <service> --also-make -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Non-root user — mandatory
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/<service>/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### JVM Container Flags (mandatory)
Always include:
- `-XX:+UseContainerSupport`: lets JVM respect cgroup memory/CPU limits
- `-XX:MaxRAMPercentage=75.0`: prevents OOM in containers (leave 25% headroom)

### Non-Root User (mandatory)
Never run application processes as `root` inside a container.

---

## Docker Compose Standards

### Health Checks (mandatory for every service)
```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:808X/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### Port Mapping
Ports must match the project port map exactly (see `CLAUDE.md` Section 2). Never introduce new port mappings without updating `CLAUDE.md`.

---

## Kubernetes Standards (current + future)

### Resource Limits (mandatory on every Deployment)
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```
Never deploy a Pod spec without `resources.limits` — it will consume unbounded node resources.

### Health Probes (mandatory)
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 808X
  initialDelaySeconds: 30
  periodSeconds: 10

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 808X
  initialDelaySeconds: 60
  periodSeconds: 15
```
Both `readiness` and `liveness` probes are mandatory. Use Spring Actuator endpoints.

### ConfigMap vs Secret
- `ConfigMap`: non-sensitive configuration (feature flags, timeouts, topic names, URLs).
- `Secret`: sensitive values (passwords, API keys, JWT signing keys, client secrets).
- Never put a password or token in a `ConfigMap`.
- Reference Secrets in Pods via `envFrom` or `env.valueFrom.secretKeyRef` — never mount as plain files.

### HorizontalPodAutoscaler
- Define HPA for any service that receives external traffic (api-gateway, user-service, product-service, order-service).
- Minimum replicas: 2 (for availability). Maximum: based on load testing results.
- Scale on CPU utilization: 70% target.
