package com.monat.ecommerce.order.domain.service;

import com.monat.ecommerce.events.order.OrderCancelledEvent;
import com.monat.ecommerce.events.order.OrderCompletedEvent;
import com.monat.ecommerce.grpc.inventory.*;
import com.monat.ecommerce.grpc.payment.PaymentServiceGrpc;
import com.monat.ecommerce.grpc.payment.ProcessPaymentRequest;
import com.monat.ecommerce.grpc.payment.ProcessPaymentResponse;
import com.monat.ecommerce.grpc.payment.RefundPaymentRequest;
import com.monat.ecommerce.grpc.user.UserServiceGrpc;
import com.monat.ecommerce.grpc.user.ValidateUserRequest;
import com.monat.ecommerce.grpc.user.ValidateUserResponse;
import com.monat.ecommerce.order.domain.model.*;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.domain.repository.OrderSagaStateRepository;
import com.monat.ecommerce.order.domain.repository.OutboxEventRepository;
import com.monat.ecommerce.order.infrastructure.client.CartClient;
import com.monat.ecommerce.order.infrastructure.config.OrderMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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
    private final OrderMetrics orderMetrics;
    private final CartClient cartClient;
    private final ObservationRegistry observationRegistry;

    @GrpcClient("inventory-service")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryService;

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentService;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userService;

    @Async("sagaTaskExecutor")
    @Transactional
    public void executeOrderSaga(UUID orderId, String cartId) {
        Timer.Sample sample = Timer.start();
        log.info("Starting Saga for order: {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderSagaState sagaState = OrderSagaState.builder()
                .orderId(order.getId())
                .currentStep(SagaStep.ORDER_CREATED)
                .status(SagaStatus.STARTED)
                .build();
        sagaStateRepository.save(sagaState);

        Observation sagaObs = Observation.createNotStarted("order.saga", observationRegistry)
                .lowCardinalityKeyValue("orderId", order.getId().toString())
                .lowCardinalityKeyValue("userId", order.getUserId().toString())
                .start();
        try {
            observeStep("order.saga.validate-user", order.getId().toString(),
                    () -> validateUser(order, sagaState));

            observeStep("order.saga.reserve-stock", order.getId().toString(),
                    () -> reserveStock(order, sagaState));

            observeStep("order.saga.process-payment", order.getId().toString(),
                    () -> processPayment(order, sagaState));

            observeStep("order.saga.complete", order.getId().toString(),
                    () -> completeOrder(order, sagaState, cartId));

            orderMetrics.incrementSagaResult("success");
            sagaObs.stop();
        } catch (Exception e) {
            log.error("Saga failed for order: {}", order.getId(), e);
            orderMetrics.incrementSagaResult("failed");
            sagaObs.error(e).stop();

            Observation compensateObs = Observation.createNotStarted("order.saga.compensate", observationRegistry)
                    .lowCardinalityKeyValue("orderId", order.getId().toString())
                    .lowCardinalityKeyValue("reason", e.getClass().getSimpleName())
                    .start();
            try {
                compensateSaga(order, sagaState, e.getMessage());
                compensateObs.stop();
            } catch (Exception ce) {
                compensateObs.error(ce).stop();
            }
        } finally {
            sample.stop(orderMetrics.sagaExecutionTimer());
        }
    }

    private void observeStep(String name, String orderId, Runnable step) {
        Observation obs = Observation.createNotStarted(name, observationRegistry)
                .lowCardinalityKeyValue("orderId", orderId)
                .start();
        try {
            step.run();
            obs.stop();
        } catch (Exception e) {
            obs.error(e).stop();
            throw e;
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
                orderMetrics.incrementSagaStep("validate_user", "failed");
                throw new RuntimeException("User validation failed: " + response.getMessage());
            }

            sagaState.moveToNextStep(SagaStep.USER_VALIDATED);
            sagaStateRepository.save(sagaState);

            log.info("User validated successfully for order: {}", order.getId());
            orderMetrics.incrementSagaStep("validate_user", "success");

        } catch (StatusRuntimeException e) {
            orderMetrics.incrementSagaStep("validate_user", "error");
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
                orderMetrics.incrementSagaStep("reserve_stock", "failed");
                throw new RuntimeException("Stock reservation failed: " + response.getMessage());
            }

            sagaState.setReservationId(response.getReservationId());
            sagaState.moveToNextStep(SagaStep.STOCK_RESERVED);
            sagaStateRepository.save(sagaState);

            log.info("Stock reserved successfully for order: {}", order.getId());
            orderMetrics.incrementSagaStep("reserve_stock", "success");

        } catch (StatusRuntimeException e) {
            orderMetrics.incrementSagaStep("reserve_stock", "error");
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
                orderMetrics.incrementSagaStep("process_payment", "failed");
                throw new RuntimeException("Payment processing failed: " + response.getMessage());
            }

            sagaState.setPaymentId(response.getPaymentId());
            sagaState.moveToNextStep(SagaStep.PAYMENT_PROCESSED);
            sagaStateRepository.save(sagaState);

            order.setPaymentReference(response.getPaymentReference());
            orderRepository.save(order);

            log.info("Payment processed successfully for order: {}", order.getId());
            orderMetrics.incrementSagaStep("process_payment", "success");

        } catch (StatusRuntimeException e) {
            orderMetrics.incrementSagaStep("process_payment", "error");
            throw new RuntimeException("Payment service unavailable: " + e.getMessage(), e);
        }
    }

    private void completeOrder(Order order, OrderSagaState sagaState, String cartId) {
        log.debug("Completing order: {}", order.getId());

        // Commit stock reservation
        try {
            CommitStockRequest request = CommitStockRequest.newBuilder()
                    .setReservationId(sagaState.getReservationId())
                    .setOrderId(order.getId().toString())
                    .build();

            inventoryService.commitStock(request);
            orderMetrics.incrementSagaStep("commit_stock", "success");
        } catch (Exception e) {
            log.warn("Failed to commit stock, but order is already paid: {}", e.getMessage());
            // Continue as payment is already processed
            orderMetrics.incrementSagaStep("commit_stock", "error");
        }

        // Mark order as completed
        order.markAsCompleted();
        orderRepository.save(order);

        // Mark saga as completed
        sagaState.moveToNextStep(SagaStep.ORDER_COMPLETED);
        sagaState.markAsCompleted();
        sagaStateRepository.save(sagaState);

        log.info("Order completed successfully: {}", order.getId());
        orderMetrics.incrementSagaStep("complete_order", "success");

        // Publish OrderCompletedEvent via outbox
        publishOrderCompletedEvent(order);

        // Delete cart only after order is confirmed complete
        if (cartId != null && !cartId.isBlank()) {
            try {
                cartClient.deleteCart(cartId);
                log.info("Cart deleted after successful order: {}", cartId);
            } catch (Exception e) {
                log.warn("Failed to delete cart {} after order completion: {}", cartId, e.getMessage());
            }
        }
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
            orderMetrics.incrementSagaStep("release_stock", "success");

        } catch (Exception e) {
            log.error("Failed to release stock for order: {}", order.getId(), e);
            // Log but don't fail compensation
            orderMetrics.incrementSagaStep("release_stock", "error");
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
            orderMetrics.incrementSagaStep("refund_payment", "success");

        } catch (Exception e) {
            log.error("Failed to refund payment for order: {}", order.getId(), e);
            // Log but don't fail compensation
            orderMetrics.incrementSagaStep("refund_payment", "error");
        }
    }

    private void publishOrderCompletedEvent(Order order) {
        try {
            OrderCompletedEvent event =
                    OrderCompletedEvent.builder()
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
            OrderCancelledEvent event =
                    OrderCancelledEvent.builder()
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
