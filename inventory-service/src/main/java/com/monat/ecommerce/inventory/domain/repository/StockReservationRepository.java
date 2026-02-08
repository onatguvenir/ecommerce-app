package com.monat.ecommerce.inventory.domain.repository;

import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    Optional<StockReservation> findByReservationId(String reservationId);

    List<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoffTime);

    List<StockReservation> findByProductIdAndStatus(String productId, ReservationStatus status);
}
