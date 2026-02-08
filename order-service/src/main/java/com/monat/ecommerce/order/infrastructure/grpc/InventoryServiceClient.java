package com.monat.ecommerce.order.infrastructure.grpc;

import com.monat.ecommerce.grpc.inventory.ReserveStockRequest;
import com.monat.ecommerce.grpc.inventory.ReserveStockResponse;
import com.monat.ecommerce.grpc.inventory.ReleaseStockRequest;
import com.monat.ecommerce.grpc.inventory.ReleaseStockResponse;
import com.monat.ecommerce.grpc.inventory.InventoryServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC client for Inventory Service with Resilience4j
 */
@Service
@Slf4j
public class InventoryServiceClient {

    @Value("${grpc.client.inventory-service.host:localhost}")
    private String host;

    @Value("${grpc.client.inventory-service.port:9083}")
    private int port;

    private ManagedChannel channel;
    private InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        stub = InventoryServiceGrpc.newBlockingStub(channel);
        log.info("Inventory Service gRPC client initialized: {}:{}", host, port);
    }

    @PreDestroy
    public void destroy() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            log.info("Inventory Service gRPC channel shut down");
        }
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventory-service")
    public ReserveStockResponse reserveStock(String orderId, java.util.Map<String, Integer> productQuantities) {
        log.info("Reserving stock for order: {}", orderId);

        var requestBuilder = ReserveStockRequest.newBuilder()
                .setOrderId(orderId);

        // Add stock items
        for (var entry : productQuantities.entrySet()) {
            requestBuilder.addItems(
                    com.monat.ecommerce.grpc.inventory.StockItem.newBuilder()
                            .setProductId(entry.getKey())
                            .setQuantity(entry.getValue())
                            .build());
        }

        ReserveStockResponse response = stub.reserveStock(requestBuilder.build());
        log.info("Reserve stock response for order {}: success={}", orderId, response.getSuccess());

        return response;
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseStockFallback")
    @Retry(name = "inventory-service")
    public ReleaseStockResponse releaseStock(String reservationId, String orderId, String reason) {
        log.info("Releasing stock reservation: {}, orderId={}", reservationId, orderId);

        ReleaseStockRequest request = ReleaseStockRequest.newBuilder()
                .setReservationId(reservationId)
                .setOrderId(orderId)
                .setReason(reason)
                .build();

        ReleaseStockResponse response = stub.releaseStock(request);
        log.info("Release stock response for order {}: success={}", orderId, response.getSuccess());

        return response;
    }

    /**
     * Fallback method when inventory service is unavailable
     */
    private ReserveStockResponse reserveStockFallback(String orderId, java.util.Map<String, Integer> productQuantities,
            Exception ex) {
        log.error("Inventory Service circuit breaker activated for reserve. Order: {}. Error: {}",
                orderId, ex.getMessage());

        return ReserveStockResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Inventory service temporarily unavailable. Please try again later.")
                .build();
    }

    /**
     * Fallback method for release stock
     */
    private ReleaseStockResponse releaseStockFallback(String reservationId, String orderId, String reason,
            Exception ex) {
        log.error("Inventory Service circuit breaker activated for release. Order: {}. Error: {}",
                orderId, ex.getMessage());

        return ReleaseStockResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Inventory service temporarily unavailable")
                .build();
    }
}
