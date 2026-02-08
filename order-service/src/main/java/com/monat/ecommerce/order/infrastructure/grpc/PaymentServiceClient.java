package com.monat.ecommerce.order.infrastructure.grpc;

import com.monat.ecommerce.grpc.payment.ProcessPaymentRequest;
import com.monat.ecommerce.grpc.payment.ProcessPaymentResponse;
import com.monat.ecommerce.grpc.payment.PaymentServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;

/**
 * gRPC client for Payment Service with Resilience4j
 */
@Service
@Slf4j
public class PaymentServiceClient {

    @Value("${grpc.client.payment-service.host:localhost}")
    private String host;

    @Value("${grpc.client.payment-service.port:9086}")
    private int port;

    private ManagedChannel channel;
    private PaymentServiceGrpc.PaymentServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        stub = PaymentServiceGrpc.newBlockingStub(channel);
        log.info("Payment Service gRPC client initialized: {}:{}", host, port);
    }

    @PreDestroy
    public void destroy() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            log.info("Payment Service gRPC channel shut down");
        }
    }

    @CircuitBreaker(name = "payment-service", fallbackMethod = "processPaymentFallback")
    @Retry(name = "payment-service")
    public ProcessPaymentResponse processPayment(String orderId, String userId, BigDecimal amount,
            String currency, String paymentMethod, String idempotencyKey) {
        log.info("Calling Payment Service for order: {}", orderId);

        ProcessPaymentRequest request = ProcessPaymentRequest.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .setAmount(amount.doubleValue())
                .setCurrency(currency)
                .setPaymentMethod(paymentMethod)
                .setIdempotencyKey(idempotencyKey)
                .build();

        ProcessPaymentResponse response = stub.processPayment(request);
        log.info("Payment response for order {}: {}", orderId, response.getStatus());

        return response;
    }

    /**
     * Fallback method when payment service is unavailable
     */
    private ProcessPaymentResponse processPaymentFallback(String orderId, String userId, BigDecimal amount,
            String currency, String paymentMethod, String idempotencyKey,
            Exception ex) {
        log.error("Payment Service circuit breaker activated for order: {}. Error: {}",
                orderId, ex.getMessage());

        return ProcessPaymentResponse.newBuilder()
                .setSuccess(false)
                .setStatus(com.monat.ecommerce.grpc.payment.PaymentStatus.FAILED)
                .setMessage("Payment service temporarily unavailable. Please try again later.")
                .build();
    }
}
