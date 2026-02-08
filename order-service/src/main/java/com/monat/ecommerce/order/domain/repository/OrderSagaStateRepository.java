package com.monat.ecommerce.order.domain.repository;

import com.monat.ecommerce.order.domain.model.OrderSagaState;
import com.monat.ecommerce.order.domain.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderSagaStateRepository extends JpaRepository<OrderSagaState, UUID> {

    Optional<OrderSagaState> findByOrderId(UUID orderId);

    List<OrderSagaState> findByStatus(SagaStatus status);

    List<OrderSagaState> findByStatusAndCreatedAtBefore(SagaStatus status, LocalDateTime cutoffTime);
}
