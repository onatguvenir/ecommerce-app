---
type: ai-example
topic: grpc-client-setup
last-updated: 2026-04-27
---

# Example: gRPC Blocking Stub Client

Based on actual implementation in order-service.

## 1. gRPC Client Injection (via @GrpcClient)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    @GrpcClient("inventory-service")  // matches grpc.client.inventory-service.* in yaml
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryService;

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userService;
}
```

## 2. Making a gRPC Call with Error Handling

```java
private void reserveStock(Order order, OrderSagaState sagaState) {
    try {
        List<StockItem> stockItems = order.getItems().stream()
            .map(item -> StockItem.newBuilder()
                .setProductId(item.getProductId())
                .setQuantity(item.getQuantity())
                .build())
            .toList();

        ReserveStockRequest request = ReserveStockRequest.newBuilder()
            .setOrderId(order.getId().toString())
            .addAllItems(stockItems)
            .build();

        ReserveStockResponse response = inventoryService.reserveStock(request);

        if (!response.getSuccess()) {
            throw new RuntimeException("Stock reservation failed: " + response.getMessage());
        }

        sagaState.setReservationId(response.getReservationId());

    } catch (StatusRuntimeException e) {
        // gRPC transport/service error
        throw new RuntimeException("Inventory service unavailable: " + e.getMessage(), e);
    }
}
```

## 3. application.yml Client Config

```yaml
grpc:
  client:
    inventory-service:
      host: ${INVENTORY_SERVICE_HOST:localhost}
      port: 9083
      negotiationType: plaintext    # dev only; TLS in prod
    payment-service:
      host: ${PAYMENT_SERVICE_HOST:localhost}
      port: 9086
      negotiationType: plaintext
    user-service:
      host: ${USER_SERVICE_HOST:localhost}
      port: 9081
      negotiationType: plaintext
```

## 4. gRPC Client with Circuit Breaker (notification-service pattern)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    @Retry(name = "user-service")
    public Optional<User> getUser(String userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();
            User user = stub.getUser(request);
            return Optional.of(user);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw new RuntimeException("User service error: " + e.getMessage(), e);
        }
    }

    private Optional<User> getUserFallback(String userId, Exception ex) {
        log.warn("User service circuit open, returning empty for userId={}", userId);
        return Optional.empty();
    }
}
```

## 5. gRPC Server Implementation Pattern

```java
@GrpcService
@Slf4j
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryDomainService inventoryService;

    @Override
    public void reserveStock(ReserveStockRequest request,
                             StreamObserver<ReserveStockResponse> responseObserver) {
        try {
            String reservationId = inventoryService.reserve(
                UUID.fromString(request.getOrderId()),
                request.getItemsList()
            );

            responseObserver.onNext(ReserveStockResponse.newBuilder()
                .setSuccess(true)
                .setReservationId(reservationId)
                .setMessage("Stock reserved")
                .build());
            responseObserver.onCompleted();

        } catch (InsufficientStockException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in reserveStock", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error")
                .asRuntimeException());
        }
    }
}
```
