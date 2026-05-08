---
type: ai-context
scope: domain-spec
service: cart-service
port-http: 8084
port-grpc: none
db: Redis (primary store — not a cache)
last-updated: 2026-04-27
---

# cart-service Domain Spec

## Responsibility
Manages shopping cart state. Redis is the primary persistence layer (not a cache). Distributed locking for concurrent cart mutations.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `CartApplicationService` | `application/service/` | Cart business logic |
| `CartController` | `infrastructure/controller/` | REST endpoints |
| `CartLockService` | `infrastructure/config/` | Redis distributed lock for cart operations |
| `Cart` | `domain/model/` | Cart aggregate with items |
| `CartItem` | `domain/model/` | Individual cart item |
| `CartRepository` | `domain/repository/` | Interface (Redis-backed) |

## Redis Storage
Cart stored as Redis hash/JSON by `cartId`.  
No PostgreSQL. Redis IS the database.  
TTL: carts expire after configured duration (set in `application.yml`).

## Distributed Lock
`CartLockService` uses Redis SETNX-style lock to prevent concurrent modification of same cart.  
Used for all write operations (add item, remove item, update quantity).

## REST Endpoints
```
POST   /api/v1/carts                   createCart
GET    /api/v1/carts/{cartId}          getCart
POST   /api/v1/carts/{cartId}/items    addItem
PUT    /api/v1/carts/{cartId}/items/{productId}   updateItemQuantity
DELETE /api/v1/carts/{cartId}/items/{productId}   removeItem
DELETE /api/v1/carts/{cartId}          deleteCart (called by order-service after saga completes)
```

## Lifecycle
1. User creates cart → `POST /api/v1/carts`
2. User adds items → `POST /api/v1/carts/{id}/items`
3. Order created → `GET /api/v1/carts/{id}` (order-service fetches cart items)
4. Saga completes → `DELETE /api/v1/carts/{id}` (order-service deletes cart)

## InsufficientStockException
`cart-service` checks stock availability when adding items (calls inventory-service or uses price from product snapshot). Throws `InsufficientStockException` → 409 Conflict.

## Important Notes
- Cart is ephemeral. Loss of Redis = loss of carts. No recovery mechanism.
- Price in cart is snapshot at time of add. Price may be stale at checkout.
- Concurrent add from two browser tabs: protected by `CartLockService`.
