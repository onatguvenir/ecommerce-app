# gRPC Service Conventions

## Active gRPC Services
- `user-service`: HTTP `8081`, gRPC `9081`
- `inventory-service`: HTTP `8083`, gRPC `9083`
- `payment-service`: HTTP `8086`, gRPC `9086`

## Proto3 Schema Rules
- One `.proto` file per service domain — placed in `grpc-proto/` module at project root.
- Package name: `com.monat.ecommerce.<service>` (match Java package).
- Use `UUID` as `string` in proto — never as `bytes`.
- All request messages must have a `request_id` field (string, UUID) for idempotency tracking.
- All response messages must include a `status` field (enum or string) and an optional `error_message`.

## Service Naming
- Service name: `<Domain>Service` (e.g., `InventoryService`)
- RPC methods: `PascalCase` verb+noun (e.g., `ReserveStock`, `GetUserById`)
- Message types: `<Rpc>Request` and `<Rpc>Response` (e.g., `ReserveStockRequest`, `ReserveStockResponse`)

## Error Handling
- Use gRPC status codes correctly:
  - `NOT_FOUND` for missing entities
  - `INVALID_ARGUMENT` for validation failures
  - `ALREADY_EXISTS` for duplicate creation
  - `INTERNAL` only for unexpected errors (never expose internal exception messages)
- Propagate gRPC errors to REST callers as appropriate HTTP status codes in the gateway.

## Stub Usage in Clients
- Always use blocking stubs for synchronous calls within request scope.
- Use `ManagedChannelBuilder` with `usePlaintext()` only in local/dev. Production must use TLS.
- Stub beans should be `@Bean` in a `GrpcClientConfig` class — never instantiated inline.
