package com.monat.ecommerce.inventory.infrastructure.persistence.repository;

import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, UUID> {

    Optional<StockReservationEntity> findByReservationId(String reservationId);

    List<StockReservationEntity> findByOrderId(String orderId);

    List<StockReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoffTime);

    List<StockReservationEntity> findByProductIdAndStatus(String productId, ReservationStatus status);
}
