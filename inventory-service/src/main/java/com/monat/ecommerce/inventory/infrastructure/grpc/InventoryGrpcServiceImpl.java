package com.monat.ecommerce.inventory.infrastructure.grpc;

import com.monat.ecommerce.grpc.inventory.*;
import com.monat.ecommerce.inventory.domain.model.Inventory;
import com.monat.ecommerce.inventory.domain.service.InventoryDomainService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC Server implementation for Inventory Service
 * 
 * Handles inter-service communication from Order Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryDomainService inventoryDomainService;

    @Override
    public void reserveStock(ReserveStockRequest request, StreamObserver<ReserveStockResponse> responseObserver) {
        log.info("gRPC reserveStock called - Order: {}", request.getOrderId());

        try {
            // Extract product quantities
            Map<String, Integer> productQuantities = new HashMap<>();
            for (StockItem item : request.getItemsList()) {
                productQuantities.put(item.getProductId(), item.getQuantity());
            }

            // Reserve stock (handles optimistic locking internally with retry)
            String reservationId = inventoryDomainService.reserveMultipleProducts(
                    request.getOrderId(),
                    productQuantities);

            // Build success response
            ReserveStockResponse response = ReserveStockResponse.newBuilder()
                    .setSuccess(true)
                    .setReservationId(reservationId)
                    .setMessage("Stock reserved successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Stock reservation successful - Reservation ID: {}", reservationId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());

            ReserveStockResponse response = ReserveStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid request: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalStateException e) {
            log.error("Insufficient stock: {}", e.getMessage());

            ReserveStockResponse response = ReserveStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to reserve stock", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to reserve stock: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void releaseStock(ReleaseStockRequest request, StreamObserver<ReleaseStockResponse> responseObserver) {
        log.info("gRPC releaseStock called - Reservation: {}", request.getReservationId());

        try {
            inventoryDomainService.releaseReservation(request.getReservationId());

            ReleaseStockResponse response = ReleaseStockResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Stock released successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Stock release successful");

        } catch (Exception e) {
            log.error("Failed to release stock", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to release stock: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void checkStock(CheckStockRequest request, StreamObserver<CheckStockResponse> responseObserver) {
        log.debug("gRPC checkStock called - Product: {}", request.getProductId());

        try {
            Inventory inventory = inventoryDomainService.getInventory(request.getProductId());

            CheckStockResponse response = CheckStockResponse.newBuilder()
                    .setProductId(request.getProductId())
                    .setAvailableQuantity(inventory.getAvailableQuantity())
                    .setReservedQuantity(inventory.getReservedQuantity())
                    .setTotalQuantity(inventory.getTotalQuantity())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.error("Product not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            log.error("Failed to check stock", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to check stock: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void commitStock(CommitStockRequest request, StreamObserver<CommitStockResponse> responseObserver) {
        log.info("gRPC commitStock called - Reservation: {}", request.getReservationId());

        try {
            inventoryDomainService.commitReservation(request.getReservationId());

            CommitStockResponse response = CommitStockResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Stock committed successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Stock commit successful");

        } catch (Exception e) {
            log.error("Failed to commit stock", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to commit stock: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
