package com.monat.ecommerce.payment.infrastructure.grpc;

import com.monat.ecommerce.grpc.payment.*;
import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentMethod;
import com.monat.ecommerce.payment.domain.service.PaymentDomainService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;

/**
 * gRPC Server implementation for Payment Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcServiceImpl extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final PaymentDomainService paymentDomainService;

    @Override
    public void processPayment(ProcessPaymentRequest request, StreamObserver<ProcessPaymentResponse> responseObserver) {
        log.info("gRPC processPayment called - Order: {}, Amount: {}", request.getOrderId(), request.getAmount());

        try {
            // Parse payment method
            PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());

            // Process payment with idempotency
            Payment payment = paymentDomainService.processPayment(
                    request.getIdempotencyKey(),
                    request.getOrderId(),
                    request.getUserId(),
                    BigDecimal.valueOf(request.getAmount()),
                    request.getCurrency(),
                    paymentMethod);

            // Build response
            ProcessPaymentResponse.Builder responseBuilder = ProcessPaymentResponse.newBuilder()
                    .setPaymentId(payment.getId().toString());

            if (payment.isCompleted()) {
                responseBuilder
                        .setSuccess(true)
                        .setStatus(com.monat.ecommerce.grpc.payment.PaymentStatus.COMPLETED)
                        .setPaymentReference(payment.getPaymentReference())
                        .setMessage("Payment processed successfully");
            } else {
                responseBuilder
                        .setSuccess(false)
                        .setStatus(com.monat.ecommerce.grpc.payment.PaymentStatus.FAILED)
                        .setMessage("Payment failed: " + payment.getFailureReason());
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            log.info("Payment processing completed - Success: {}", payment.isCompleted());

        } catch (IllegalArgumentException e) {
            log.error("Invalid payment request: {}", e.getMessage());

            ProcessPaymentResponse response = ProcessPaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid request: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to process payment", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process payment: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void refundPayment(RefundPaymentRequest request, StreamObserver<RefundPaymentResponse> responseObserver) {
        log.info("gRPC refundPayment called - Payment ID: {}", request.getPaymentId());

        try {
            Payment payment = paymentDomainService.refundPayment(
                    request.getPaymentId(),
                    request.getOrderId(),
                    BigDecimal.valueOf(request.getAmount()),
                    request.getReason());

            RefundPaymentResponse response = RefundPaymentResponse.newBuilder()
                    .setSuccess(true)
                    .setRefundTransactionId(payment.getRefundReference())
                    .setMessage("Refund processed successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Refund successful - Reference: {}", payment.getRefundReference());

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Refund request failed: {}", e.getMessage());

            RefundPaymentResponse response = RefundPaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to refund payment", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to refund payment: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPaymentStatus(GetPaymentStatusRequest request,
            StreamObserver<GetPaymentStatusResponse> responseObserver) {
        log.debug("gRPC getPaymentStatus called - Payment ID: {}", request.getPaymentId());

        try {
            Payment payment = paymentDomainService.getPaymentStatus(request.getPaymentId());

            // Map domain PaymentStatus to gRPC PaymentStatus
            com.monat.ecommerce.grpc.payment.PaymentStatus grpcStatus;
            switch (payment.getStatus()) {
                case COMPLETED:
                    grpcStatus = com.monat.ecommerce.grpc.payment.PaymentStatus.COMPLETED;
                    break;
                case FAILED:
                    grpcStatus = com.monat.ecommerce.grpc.payment.PaymentStatus.FAILED;
                    break;
                case REFUNDED:
                    grpcStatus = com.monat.ecommerce.grpc.payment.PaymentStatus.REFUNDED;
                    break;
                default:
                    grpcStatus = com.monat.ecommerce.grpc.payment.PaymentStatus.PENDING;
            }

            GetPaymentStatusResponse response = GetPaymentStatusResponse.newBuilder()
                    .setPaymentId(payment.getId().toString())
                    .setStatus(grpcStatus)
                    .setAmount(payment.getAmount().doubleValue())
                    .setCurrency(payment.getCurrency())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.error("Payment not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("Failed to get payment status", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get payment status: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
