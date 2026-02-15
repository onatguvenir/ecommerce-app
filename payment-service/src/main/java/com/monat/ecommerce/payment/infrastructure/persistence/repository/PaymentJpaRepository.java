package com.monat.ecommerce.payment.infrastructure.persistence.repository;

import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentEntity> findByPaymentReference(String paymentReference);

    List<PaymentEntity> findByOrderId(String orderId);

    List<PaymentEntity> findByUserId(String userId);

    List<PaymentEntity> findByStatus(PaymentStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
