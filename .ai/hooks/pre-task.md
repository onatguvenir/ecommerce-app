---
type: ai-hook
trigger: before-any-task
last-updated: 2026-04-27
---

# Pre-Task Context Loading

Before starting any task, determine scope and load relevant context.

## Step 1: Identify Affected Service(s)
From user request, determine which service(s) are involved.  
Each service is a separate Maven module. Changes are isolated per module.

| Keyword in request | Service to focus on |
|---|---|
| order, saga, cart deletion | order-service |
| payment, refund, charge | payment-service |
| notification, email, SMS | notification-service |
| product, catalog, search, GraphQL | product-service |
| stock, inventory, reservation | inventory-service |
| cart, shopping cart | cart-service |
| user, auth, JWT, login | user-service |
| fraud, suspicious | fraud-service |
| gateway, routing, rate limit | api-gateway |

## Step 2: Load Service Domain Spec
Read `docs/ai/domain-specs/<service>.md` for the affected service.

## Step 3: Check Relevant Rules
- Kafka publishing → read `docs/ai/backend-rules.md` section 1
- Lock strategy → read `docs/ai/backend-rules.md` section 2
- New endpoint → read `docs/ai/coding-standards.md` + REST rules
- gRPC changes → read `docs/ai/playbooks/add-grpc-method.md`
- New Kafka event → read `docs/ai/playbooks/add-kafka-event.md`

## Step 4: Check Remaining Issues
Read `docs/ai/remaining-issues.md` to avoid building on known broken assumptions.

## Step 5: Identify Key Files
Use the class table in the domain-spec to locate relevant files.  
Pattern: `<service>/src/main/java/com/monat/ecommerce/<service>/<layer>/<class>.java`

## Do NOT
- Access other service's database tables directly
- Call `kafkaTemplate.send()` inside `@Transactional` service methods
- Use `synchronized` keyword with virtual threads
- Add `@Transactional` to repository or controller layer
- Return `null` from methods — use `Optional` or throw exception
