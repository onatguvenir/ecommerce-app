package com.monat.ecommerce.order.infrastructure.persistence.repository;

import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.infrastructure.persistence.entity.OrderEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Page<OrderEntity> findByUserId(UUID userId, Pageable pageable);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM OrderEntity o WHERE o.userId = :userId AND o.status = :status")
    List<OrderEntity> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt < :cutoffTime AND o.status = 'PENDING'")
    List<OrderEntity> findPendingOrdersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    long countByUserId(UUID userId);

    long countByStatus(OrderStatus status);
}
