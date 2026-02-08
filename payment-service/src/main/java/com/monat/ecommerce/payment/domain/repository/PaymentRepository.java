package com.monat.ecommerce.payment.domain.repository;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
