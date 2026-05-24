# Monat E-Commerce Project Guide


## Wiki Path When you need information about me or my business: 
1. Go to C:\Users\Monat\Documents\Obsidian Vault\monat-ecommerce\ 
2. hot.md -> most recent info 
3. index.md -> list of all pages 
4. Read wiki pages as needed Do not read the wiki unless you actually need it.

## Wiki update
update the wiki when project changed and updating is needed

## AI Documentation
Structured docs optimized for AI agents. Read these before working on any task.

### When to Read What
| Situation | File |
|---|---|
| Starting any task | `.ai/hooks/pre-task.md` |
| Before editing files | `.ai/hooks/editing-rules.md` |
| Cross-service work | `.ai/hooks/service-boundaries.md` |
| Current build/feature status | `docs/ai/current-state.md` |
| Ports, services, patterns | `docs/ai/architecture.md` |
| Who owns what data | `docs/ai/business-boundaries.md` |
| Hard backend rules (outbox, locks) | `docs/ai/backend-rules.md` |
| Java/Spring coding conventions | `docs/ai/coding-standards.md` |
| Known bugs and tech debt | `docs/ai/remaining-issues.md` |
| Per-service details | `docs/ai/domain-specs/<service>.md` |
| Adding Kafka event | `docs/ai/playbooks/add-kafka-event.md` |
| Adding gRPC method | `docs/ai/playbooks/add-grpc-method.md` |
| Debugging saga failures | `docs/ai/playbooks/debug-saga.md` |
| Adding new microservice | `docs/ai/playbooks/add-service.md` |
| Outbox code example | `docs/ai/examples/outbox-implementation.md` |
| gRPC client code example | `docs/ai/examples/grpc-client.md` |
| Saga step code example | `docs/ai/examples/saga-step.md` |

### Domain Specs (per service)
`docs/ai/domain-specs/`: order-service, payment-service, notification-service, product-service, inventory-service, cart-service, user-service, fraud-service

[//]: # (## 1. Mimariye Genel Bakış)
## Mimariye Genel Bakış
Monat E-Commerce Event-Driven, Microservices tabanlı, Spring Boot 3.x ve JDK 21 mimarisine sahip bir projedir.

[//]: # ()
[//]: # (## 2. Port ve Servis Haritası)

[//]: # (Herhangi bir **Port Çakışması &#40;Conflict&#41; YOKTUR**. Tüm HTTP ve gRPC portları benzersiz atanmıştır:)

[//]: # ()
[//]: # (### Altyapı &#40;Infrastructure&#41;)

[//]: # (- **PostgreSQL**: 5432)

[//]: # (- **MongoDB**: 27017)

[//]: # (- **Redis**: 6379 )

[//]: # (- **Zookeeper**: 2181)

[//]: # (- **Kafka**: 9092)

[//]: # (- **AKHQ &#40;Kafka UI&#41;**: 9000)

[//]: # (- **RedisInsight &#40;Redis UI&#41;**: 8001)

[//]: # (- **Prometheus**: 9090)

[//]: # (- **Grafana**: 3000)

[//]: # (- **Jaeger**: 16686 &#40;UI&#41;)

[//]: # (- **ELK Stack**: 9200 &#40;ES&#41;, 5044 &#40;Logstash&#41;, 5601 &#40;Kibana&#41;)

[//]: # ()
[//]: # (### Mikroservisler)

[//]: # (| Servis Adı | HTTP Portu | gRPC Portu | DB / Cache / Event |)

[//]: # (|---|---|---|---|)

[//]: # (| **api-gateway** | `8080` | - | Redis &#40;Rate limiting&#41; |)

[//]: # (| **user-service** | `8081` | `9081` | PostgreSQL |)

[//]: # (| **product-service**| `8082` | - | MongoDB, Elastic, Redis |)

[//]: # (| **inventory-service**| `8083`| `9083` | PostgreSQL, Redis |)

[//]: # (| **cart-service** | `8084` | - | Redis |)

[//]: # (| **order-service** | `8085` | - | PostgreSQL, Kafka &#40;Outbox&#41; |)

[//]: # (| **payment-service**| `8086` | `9086` | PostgreSQL, Kafka &#40;Outbox&#41;|)

[//]: # (| **notification-service**| `8087`|- | Kafka &#40;Listener&#41; |)

[//]: # ()
[//]: # (## 3. Kodlama Standartları ve Prensipleri)

[//]: # (Aşağıdaki kurallar Agent prompt maliyetlerini düşürmek ve sistem sağlığını korumak için sıkı bir şekilde uygulanmalıdır:)

[//]: # (- **Teknoloji**: Java 21, Spring Boot 3.x, Virtual Threads &#40;`spring.threads.virtual.enabled=true`&#41;.)

[//]: # (- **Idempotency & Concurrency**: Finansal ve kritik veritabanı işlemlerinde `@Lock&#40;LockModeType.PESSIMISTIC_WRITE&#41;` kullanılmalıdır.  Gerekmediği durumlarda &#40;sadece update&#41; JPA `@Version` optimistic kilidi standarttır.)

[//]: # (- **Event-Driven İletişimi**: `KafkaTemplate` ile _doğrudan_ veri aktarımı yasaktır. Çift yazma &#40;dual-write&#41; problemini çözmek için **Transactional Outbox Pattern** ve `@Scheduled` tablolar kullanılacaktır.)

[//]: # (- **Null Safety**: Hata fırlatmalar izolasyonlu &#40;Guard Clauses, `Optional`, `@NotNull`&#41;, nesneler mümkünse **Immutable** veya `Record` olmalıdır. )

[//]: # ()
[//]: # (## 4. Kullanışlı Komutlar)

[//]: # (- Sadece `payment-service` build: `mvn compile -pl payment-service --also-make -q`)

[//]: # (- Tüm Docker mimarisini kurma: `docker compose up -d`)

[//]: # (- Log inceleme: `docker compose logs -f [service_name]`)

[//]: # (## 5. Detaylı Kurallar)

[//]: # ()
[//]: # (Aşağıdaki kural dosyaları bu projeye uygulanır:)

[//]: # ()
[//]: # (@.claude/rules/coding-standards.md)

[//]: # (@.claude/rules/concurrency.md)

[//]: # (@.claude/rules/null-safety.md)

[//]: # (@.claude/rules/outbox-pattern.md)

[//]: # (@.claude/rules/grpc-conventions.md)

[//]: # (@.claude/rules/api-conventions.md)

[//]: # (@.claude/rules/infrastructure.md)



# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- ALWAYS read graphify-out/GRAPH_REPORT.md before reading any source files, running grep/glob searches, or answering codebase questions. The graph is your primary map of the codebase.
- IF graphify-out/wiki/index.md EXISTS, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
