package com.monat.ecommerce.order.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.grpc.inventory.*;
import com.monat.ecommerce.grpc.payment.*;
import com.monat.ecommerce.grpc.user.*;
import com.monat.ecommerce.order.domain.model.*;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.repository.OrderSagaStateRepository;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Saga Orchestrator for managing distributed transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final OrderSagaStateRepository sagaStateRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryService;

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userService;

    @Transactional
    public void executeOrderSaga(Order order) {
        log.info("Starting Saga for order: {}", order.getId());

        // Create Saga state
        OrderSagaState sagaState = OrderSagaState.builder()
                .orderId(order.getId())
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.STARTED)
                .build();
        sagaStateRepository.save(sagaState);

        try {
            // Step 1: Validate User
            validateUser(order, sagaState);

            // Step 2: Reserve Stock
            reserveStock(order, sagaState);

            // Step 3: Process Payment
            processPayment(order, sagaState);

            // Step 4: Complete Order
            completeOrder(order, sagaState);

        } catch (Exception e) {
            log.error("Saga failed for order: {}", order.getId(), e);
            compensateSaga(order, sagaState, e.getMessage());
        }
    }

    private void validateUser(Order order, OrderSagaState sagaState) {
        log.debug("Validating user: {}", order.getUserId());

        try {
            ValidateUserRequest request = ValidateUserRequest.newBuilder()
                    .setUserId(order.getUserId().toString())
                    .build();

            ValidateUserResponse response = userService.validateUser(request);

            if (!response.getIsValid() || !response.getIsActive()) {
                throw new RuntimeException("User validation failed: " + response.getMessage());
            }

            sagaState.moveToNextStep(SagaStep.USER_VALIDATED);
            sagaStateRepository.save(sagaState);

            log.info("User validated successfully for order: {}", order.getId());

        } catch (StatusRuntimeException e) {
            throw new RuntimeException("User service unavailable: " + e.getMessage(), e);
        }
    }

    private void reserveStock(Order order, OrderSagaState sagaState) {
        log.debug("Reserving stock for order: {}", order.getId());

        try {
            List<StockItem> stockItems = order.getItems().stream()
                    .map(item -> StockItem.newBuilder()
                            .setProductId(item.getProductId())
                            .setQuantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            ReserveStockRequest request = ReserveStockRequest.newBuilder()
                    .setOrderId(order.getId().toString())
                    .addAllItems(stockItems)
                    .build();

            ReserveStockResponse response = inventoryService.reserveStock(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Stock reservation failed: " + response.getMessage());
            }

            sagaState.setReservationId(response.getReservationId());
            sagaState.moveToNextStep(SagaStep.STOCK_RESERVED);
            sagaStateRepository.save(sagaState);

            log.info("Stock reserved successfully for order: {}", order.getId());

        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Inventory service unavailable: " + e.getMessage(), e);
        }
    }

    private void processPayment(Order order, OrderSagaState sagaState) {
        log.debug("Processing payment for order: {}", order.getId());

        try {
            ProcessPaymentRequest request = ProcessPaymentRequest.newBuilder()
                    .setOrderId(order.getId().toString())
                    .setUserId(order.getUserId().toString())
                    .setAmount(order.getTotalAmount().doubleValue())
                    .setCurrency(order.getCurrency())
                    .setPaymentMethod("CARD")
                    .setIdempotencyKey(order.getOrderNumber())
                    .build();

            ProcessPaymentResponse response = paymentService.processPayment(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Payment processing failed: " + response.getMessage());
            }

            sagaState.setPaymentId(response.getPaymentId());
            sagaState.moveToNextStep(SagaStep.PAYMENT_PROCESSED);
            sagaStateRepository.save(sagaState);

            order.setPaymentReference(response.getPaymentReference());
            orderRepository.save(order);

            log.info("Payment processed successfully for order: {}", order.getId());

        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Payment service unavailable: " + e.getMessage(), e);
        }
    }

    private void completeOrder(Order order, OrderSagaState sagaState) {
        log.debug("Completing order: {}", order.getId());

        // Commit stock reservation
        try {
            CommitStockRequest request = CommitStockRequest.newBuilder()
                    .setReservationId(sagaState.getReservationId())
                    .setOrderId(order.getId().toString())
                    .build();

            inventoryService.commitStock(request);
        } catch (Exception e) {
            log.warn("Failed to commit stock, but order is already paid: {}", e.getMessage());
            // Continue as payment is already processed
        }

        // Mark order as completed
        order.markAsCompleted();
        orderRepository.save(order);

        // Mark saga as completed
        sagaState.moveToNextStep(SagaStep.ORDER_COMPLETED);
        sagaState.markAsCompleted();
        sagaStateRepository.save(sagaState);

        log.info("Order completed successfully: {}", order.getId());

        // Publish OrderCompletedEvent via outbox
        publishOrderCompletedEvent(order);
    }

    private void compensateSaga(Order order, OrderSagaState sagaState, String errorMessage) {
        log.warn("Starting compensation for order: {}", order.getId());

        sagaState.markAsCompensating();
        sagaState.setErrorMessage(errorMessage);
        sagaStateRepository.save(sagaState);

        // Release stock if it was reserved
        if (sagaState.getReservationId() != null) {
            releaseStock(order, sagaState);
        }

        // Refund payment if it was processed
        if (sagaState.getPaymentId() != null) {
            refundPayment(order, sagaState);
        }

        // Mark order as failed
        order.markAsFailed(errorMessage);
        orderRepository.save(order);

        // Mark saga as compensated
        sagaState.moveToNextStep(SagaStep.COMPENSATION_COMPLETED);
        sagaState.markAsCompensated();
        sagaStateRepository.save(sagaState);

        log.info("Compensation completed for order: {}", order.getId());

        // Publish OrderCancelledEvent
        publishOrderCancelledEvent(order, errorMessage);
    }

    private void releaseStock(Order order, OrderSagaState sagaState) {
        log.debug("Releasing stock for order: {}", order.getId());

        try {
            ReleaseStockRequest request = ReleaseStockRequest.newBuilder()
                    .setReservationId(sagaState.getReservationId())
                    .setOrderId(order.getId().toString())
                    .setReason("Order failed: " + sagaState.getErrorMessage())
                    .build();

            inventoryService.releaseStock(request);

            sagaState.moveToNextStep(SagaStep.STOCK_RELEASED);
            sagaStateRepository.save(sagaState);

            log.info("Stock released for order: {}", order.getId());

        } catch (Exception e) {
            log.error("Failed to release stock for order: {}", order.getId(), e);
            // Log but don't fail compensation
        }
    }

    private void refundPayment(Order order, OrderSagaState sagaState) {
        log.debug("Refunding payment for order: {}", order.getId());

        try {
            RefundPaymentRequest request = RefundPaymentRequest.newBuilder()
                    .setPaymentId(sagaState.getPaymentId())
                    .setOrderId(order.getId().toString())
                    .setAmount(order.getTotalAmount().doubleValue())
                    .setReason("Order cancellation: " + sagaState.getErrorMessage())
                    .build();

            paymentService.refundPayment(request);

            sagaState.moveToNextStep(SagaStep.PAYMENT_REFUNDED);
            sagaStateRepository.save(sagaState);

            log.info("Payment refunded for order: {}", order.getId());

        } catch (Exception e) {
            log.error("Failed to refund payment for order: {}", order.getId(), e);
            // Log but don't fail compensation
        }
    }

    private void publishOrderCompletedEvent(Order order) {
        try {
            com.monat.ecommerce.events.order.OrderCompletedEvent event =
                    com.monat.ecommerce.events.order.OrderCompletedEvent.builder()
                            .orderId(order.getId().toString())
                            .orderNumber(order.getOrderNumber())
                            .userId(order.getUserId().toString())
                            .totalAmount(order.getTotalAmount())
                            .currency(order.getCurrency())
                            .paymentReference(order.getPaymentReference())
                            .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCompleted")
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            log.error("Failed to publish OrderCompletedEvent", e);
        }
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        try {
            com.monat.ecommerce.events.order.OrderCancelledEvent event =
                    com.monat.ecommerce.events.order.OrderCancelledEvent.builder()
                            .orderId(order.getId().toString())
                            .orderNumber(order.getOrderNumber())
                            .userId(order.getUserId().toString())
                            .reason(reason)
                            .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCancelled")
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            log.error("Failed to publish OrderCancelledEvent", e);
        }
    }
}
