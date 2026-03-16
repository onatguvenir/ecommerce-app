package com.monat.ecommerce.payment.infrastructure.persistence.repository;

import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.idempotencyKey = :idempotencyKey")
    Optional<PaymentEntity> findByIdempotencyKeyWithLock(@Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id")
    Optional<PaymentEntity> findByIdWithLock(@Param("id") UUID id);

    Optional<PaymentEntity> findByPaymentReference(String paymentReference);

    List<PaymentEntity> findByOrderId(String orderId);

    List<PaymentEntity> findByUserId(String userId);

    List<PaymentEntity> findByStatus(PaymentStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
