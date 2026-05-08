---
type: ai-context
scope: domain-spec
service: product-service
port-http: 8082
port-grpc: none
db: MongoDB (productdb) + Elasticsearch + Redis
last-updated: 2026-04-27
---

# product-service Domain Spec

## Responsibility
Manages product catalog using CQRS. Writes go to MongoDB; reads/search use Elasticsearch. Redis for cache. Exposes REST + GraphQL.

## CQRS Split

| Path | Store | Service |
|---|---|---|
| Commands (create/update/delete) | MongoDB | `ProductCommandService` |
| Queries (get, list, search) | Elasticsearch (fallback: MongoDB) | `ProductQueryService` |

## Key Classes

| Class | Path | Role |
|---|---|---|
| `ProductCommandService` | `application/command/` | dispatches CreateProduct/UpdateProduct/DeleteProduct commands |
| `ProductQueryService` | `application/query/` | dispatches GetProduct/SearchProducts queries |
| `CreateProductCommandHandler` | `application/command/handler/` | writes to MongoDB, publishes domain event |
| `SearchProductsQueryHandler` | `application/query/handler/` | queries Elasticsearch |
| `ProductSearchRepository` | `infrastructure/search/` | Elasticsearch client queries |
| `ProductMongoRepository` | `infrastructure/persistence/repository/` | MongoDB Spring Data |
| `DebeziumProductCacheListener` | `infrastructure/messaging/` | Listens to MongoDB CDC → updates Redis cache |
| `ProductGraphQlController` | `infrastructure/graphql/` | GraphQL queries/mutations |
| `CacheConfig` | `infrastructure/config/` | Redis cache for product reads |
| `ProductSyncService` | `domain/service/` | MongoDB → Elasticsearch sync |

## Domain Events (Spring Application Events, internal)
- `ProductCreatedEvent` → triggers Elasticsearch index creation
- `ProductUpdatedEvent` → triggers Elasticsearch document update
- `ProductDeletedEvent` → triggers Elasticsearch document deletion

## MongoDB Document: `ProductDocument`
Primary write store. Fields include: id, name, description, price, category, status, specifications (Map), stock-related info.

## Elasticsearch Document: `ProductSearchDocument`
Searchable index. Synced from MongoDB via domain events and Debezium CDC.

## Cache: Redis
- Product detail cache (`product-cache` key prefix)
- Invalidated on MongoDB changes via Debezium CDC listener
- Fallback to MongoDB if Elasticsearch unavailable (circuit breaker)

## REST Endpoints
```
POST   /api/v1/products            createProduct
GET    /api/v1/products/{id}       getProduct
PUT    /api/v1/products/{id}       updateProduct
DELETE /api/v1/products/{id}       deleteProduct
GET    /api/v1/products            listProducts (category filter, pagination)
GET    /api/v1/products/search     searchProducts (full-text, Elasticsearch)
```

## GraphQL
Available at `/graphql`. Controller: `ProductGraphQlController`.  
Supports: product queries, product mutations, paginated product lists.

## ProductStatus Enum
`ACTIVE`, `INACTIVE`, `DRAFT`

## Important Notes
- MongoDB runs as replica set (rs0) — required for Debezium CDC oplog access.
- `ProductSpecifications` is a MongoDB embedded `Map<String, String>` for flexible attributes.
- Search fallback: if Elasticsearch down, `ProductSearchRepository` falls back to MongoDB query.
