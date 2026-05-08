---
type: ai-example
topic: saga-orchestration
last-updated: 2026-04-27
---

# Example: Saga Step Implementation

Based on actual OrderSagaOrchestrator in order-service.

## Complete Saga Step Structure

```java
@Transactional
public void executeOrderSaga(Order order, String cartId) {
    // 1. Create saga state record
    OrderSagaState sagaState = OrderSagaState.builder()
        .orderId(order.getId())
        .currentStep(SagaStep.ORDER_CREATED)
        .status(SagaStatus.STARTED)
        .build();
    sagaStateRepository.save(sagaState);

    try {
        // 2. Execute steps sequentially
        validateUser(order, sagaState);
        reserveStock(order, sagaState);
        processPayment(order, sagaState);
        completeOrder(order, sagaState, cartId);
        // Success
    } catch (Exception e) {
        log.error("Saga failed for order: {}", order.getId(), e);
        compensateSaga(order, sagaState, e.getMessage());
    }
}
```

## Adding a New Saga Step

### 1. Add to SagaStep enum
```java
public enum SagaStep {
    ORDER_CREATED,
    USER_VALIDATED,
    STOCK_RESERVED,
    PAYMENT_PROCESSED,
    MY_NEW_STEP,       // add here
    ORDER_COMPLETED,
    // compensation steps...
    STOCK_RELEASED,
    MY_NEW_STEP_COMPENSATED,  // if it needs compensation
    PAYMENT_REFUNDED,
    COMPENSATION_COMPLETED
}
```

### 2. Implement step method
```java
private void myNewStep(Order order, OrderSagaState sagaState) {
    log.debug("MyNewStep for order: {}", order.getId());

    try {
        // call gRPC or REST
        MyResponse response = myServiceClient.doSomething(order.getId().toString());

        if (!response.getSuccess()) {
            throw new RuntimeException("MyNewStep failed: " + response.getMessage());
        }

        // store result in sagaState for compensation
        sagaState.setMyNewStepResultId(response.getResultId());
        sagaState.moveToNextStep(SagaStep.MY_NEW_STEP);
        sagaStateRepository.save(sagaState);

    } catch (StatusRuntimeException e) {
        throw new RuntimeException("my-service unavailable: " + e.getMessage(), e);
    }
}
```

### 3. Add compensation method
```java
private void compensateMyNewStep(Order order, OrderSagaState sagaState) {
    if (sagaState.getMyNewStepResultId() == null) return; // not executed, skip

    try {
        myServiceClient.rollback(sagaState.getMyNewStepResultId());
        sagaState.moveToNextStep(SagaStep.MY_NEW_STEP_COMPENSATED);
        sagaStateRepository.save(sagaState);
    } catch (Exception e) {
        log.error("Failed to compensate myNewStep for order: {}", order.getId(), e);
        // NEVER re-throw in compensation — saga must reach terminal state
    }
}
```

### 4. Wire into executeOrderSaga
```java
// Forward path (after existing steps, before completeOrder):
myNewStep(order, sagaState);

// Compensation path (in compensateSaga, check if step was reached):
if (sagaState.getMyNewStepResultId() != null) {
    compensateMyNewStep(order, sagaState);
}
```

## SagaState Entity Requirements
The `order_saga_state` table must have a column for each step's result ID used in compensation:
- `reservation_id` (String) — from inventory.ReserveStock
- `payment_id` (String) — from payment.ProcessPayment
- Add: `my_new_step_result_id` (String) for new steps

Add Flyway migration to add the column.

## Compensation Rules
1. Compensate in REVERSE order of execution
2. Never throw in compensation methods — log error, continue
3. Guard each compensation step: only execute if the step result ID is set
4. Always mark saga terminal: `COMPENSATION_COMPLETED` + `markAsCompensated()`
5. Always publish `order.cancelled` event (via outbox) after compensation
