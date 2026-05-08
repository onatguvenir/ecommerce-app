---
type: ai-playbook
topic: add-grpc-method
last-updated: 2026-04-27
---

# Playbook: Add a New gRPC Method

## Step 1 — Edit Proto File
File: `grpc-proto/src/main/proto/<service>-service.proto`

```protobuf
syntax = "proto3";
package com.monat.ecommerce.<service>;
option java_multiple_files = true;
option java_package = "com.monat.ecommerce.grpc.<service>";

service InventoryService {
  rpc ReserveStock(ReserveStockRequest) returns (ReserveStockResponse);
  rpc MyNewMethod(MyNewRequest) returns (MyNewResponse);  // add here
}

message MyNewRequest {
  string request_id = 1;  // mandatory: idempotency
  string some_field = 2;
}

message MyNewResponse {
  bool success = 1;
  string message = 2;
}
```

Rules:
- All request messages: include `request_id` (string UUID) for idempotency
- All response messages: include `success` (bool) + `message` (string)
- UUIDs as `string`, not `bytes`
- Method names: PascalCase verb+noun

## Step 2 — Regenerate gRPC Stubs
```bash
mvn compile -pl grpc-proto --also-make -q
```
Generated stubs go to `grpc-proto/target/generated-sources/`.

## Step 3 — Implement on Server Side
```java
// <service>/infrastructure/grpc/<Service>GrpcServiceImpl.java
@GrpcService
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    @Override
    public void myNewMethod(MyNewRequest request, StreamObserver<MyNewResponse> responseObserver) {
        try {
            // validate
            if (request.getSomeField().isBlank()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("someField required")
                    .asRuntimeException());
                return;
            }

            // business logic via service
            someService.doWork(request.getSomeField());

            responseObserver.onNext(MyNewResponse.newBuilder()
                .setSuccess(true)
                .setMessage("OK")
                .build());
            responseObserver.onCompleted();

        } catch (ResourceNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in myNewMethod", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error")
                .asRuntimeException());
        }
    }
}
```

## Step 4 — Create Client in Calling Service
```java
// <caller-service>/infrastructure/grpc/<Service>ServiceClient.java
@Component
public class InventoryServiceClient {

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public MyNewResponse callMyNewMethod(String someField) {
        try {
            return stub.myNewMethod(MyNewRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setSomeField(someField)
                .build());
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("inventory-service unavailable: " + e.getMessage(), e);
        }
    }
}
```

## Step 5 — Configure gRPC Client in application.yml
```yaml
# caller-service/src/main/resources/application.yml
grpc:
  client:
    inventory-service:
      host: localhost        # docker: use container name
      port: 9083
      negotiationType: plaintext  # dev only; use TLS in prod
```

## Checklist
- [ ] Proto file updated with new message + RPC
- [ ] `mvn compile -pl grpc-proto --also-make` succeeds
- [ ] Server impl: correct gRPC status codes (NOT_FOUND, INVALID_ARGUMENT, INTERNAL)
- [ ] Client uses blocking stub + catches StatusRuntimeException
- [ ] `@GrpcClient("service-name")` annotation with correct service name matching yaml key
- [ ] `request_id` field present in request message
