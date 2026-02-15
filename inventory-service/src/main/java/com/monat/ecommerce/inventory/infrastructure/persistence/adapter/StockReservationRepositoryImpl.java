package com.monat.ecommerce.inventory.infrastructure.persistence.adapter;

import com.monat.ecommerce.inventory.domain.model.ReservationStatus;
import com.monat.ecommerce.inventory.domain.model.StockReservation;
import com.monat.ecommerce.inventory.domain.repository.StockReservationRepository;
import com.monat.ecommerce.inventory.infrastructure.persistence.entity.StockReservationEntity;
import com.monat.ecommerce.inventory.infrastructure.persistence.mapper.StockReservationMapper;
import com.monat.ecommerce.inventory.infrastructure.persistence.repository.StockReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockReservationRepositoryImpl implements StockReservationRepository {

    private final StockReservationJpaRepository jpaRepository;
    private final StockReservationMapper mapper;

    @Override
    public StockReservation save(StockReservation reservation) {
        StockReservationEntity entity = mapper.toEntity(reservation);
        StockReservationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<StockReservation> findByReservationId(String reservationId) {
        return jpaRepository.findByReservationId(reservationId).map(mapper::toDomain);
    }

    @Override
    public List<StockReservation> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoffTime) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, cutoffTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockReservation> findByProductIdAndStatus(String productId, ReservationStatus status) {
        return jpaRepository.findByProductIdAndStatus(productId, status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
