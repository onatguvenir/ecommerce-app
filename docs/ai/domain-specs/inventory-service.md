---
type: ai-context
scope: domain-spec
service: inventory-service
port-http: 8083
port-grpc: 9083
db: PostgreSQL (inventorydb) + Redis (L2 cache) + Caffeine (L1 cache)
last-updated: 2026-04-27
---

# inventory-service Domain Spec

## Responsibility
Manages stock levels. Serves gRPC for stock reservation/commit/release. Uses L1+L2 cache.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `InventoryGrpcServiceImpl` | `infrastructure/grpc/` | gRPC server: ReserveStock, CommitStock, ReleaseStock |
| `InventoryApplicationService` | `application/service/` | Application coordination |
| `InventoryDomainService` | `domain/service/` | Stock mutation logic with pessimistic lock |
| `CacheConfig` | `infrastructure/config/` | L1 Caffeine + L2 Redis composite cache |

## gRPC Server (port 9083)

### ReserveStock
```
ReserveStockRequest { orderId, items: [{productId, quantity}] }
ReserveStockResponse { success, reservationId, message }
```
Creates a stock reservation (not committed yet). Uses `PESSIMISTIC_WRITE` lock.

### CommitStock
```
CommitStockRequest { reservationId, orderId }
CommitStockResponse { success, message }
```
Called after payment success. Converts reservation to committed deduction.

### ReleaseStock
```
ReleaseStockRequest { reservationId, orderId, reason }
ReleaseStockResponse { success, message }
```
Called in saga compensation. Releases reserved stock back.

## Cache Architecture

```
L1: Caffeine (in-process)
  - TTL: 5 minutes
  - Max size: 2000 entries
  - Cache names: "inventory", "reservations"

L2: Redis (distributed)
  - TTL: 60 minutes
  - Serializer: GenericJackson2JsonRedisSerializer
  
CompositeCacheManager: L1 → L2 → DB
```

## Locking Rule
**MANDATORY**: All stock mutation queries use `@Lock(LockModeType.PESSIMISTIC_WRITE)`.  
Never use optimistic lock for inventory. Concurrent reservation without pessimistic lock = overselling.

## DB Tables
- `inventory_items` — stock per productId, quantity
- `stock_reservations` — pending reservations: (reservationId, productId, quantity, status)

## REST Endpoints
```
GET  /api/v1/inventory/{productId}    getInventory
POST /api/v1/inventory                addInventory (admin)
PUT  /api/v1/inventory/{productId}    updateStock (admin)
```
(Primary interface is gRPC — REST is secondary/admin)
