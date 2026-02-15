package com.monat.ecommerce.inventory.domain.repository;

import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.model.StockReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    Optional<StockReservation> findByReservationId(String reservationId);

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoffTime);

    List<StockReservation> findByProductIdAndStatus(String productId, ReservationStatus status);
}
