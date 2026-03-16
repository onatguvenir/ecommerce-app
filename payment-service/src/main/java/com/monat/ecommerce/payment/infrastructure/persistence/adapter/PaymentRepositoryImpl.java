package com.monat.ecommerce.payment.infrastructure.persistence.adapter;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentStatus;
import com.monat.ecommerce.payment.domain.model.PaymentOutboxEvent;
import com.monat.ecommerce.payment.domain.repository.PaymentRepository;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentOutboxEventEntity;
import com.monat.ecommerce.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.monat.ecommerce.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.monat.ecommerce.payment.infrastructure.persistence.repository.PaymentOutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentOutboxEventJpaRepository outboxJpaRepository;
    private final PaymentMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);
        PaymentEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdWithLock(UUID id) {
        return jpaRepository.findByIdWithLock(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKeyWithLock(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKeyWithLock(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByPaymentReference(String paymentReference) {
        return jpaRepository.findByPaymentReference(paymentReference).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return jpaRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public PaymentOutboxEvent saveOutboxEvent(PaymentOutboxEvent event) {
        PaymentOutboxEventEntity entity = mapper.toOutboxEntity(event);
        PaymentOutboxEventEntity savedEntity = outboxJpaRepository.save(entity);
        return mapper.toOutboxDomain(savedEntity);
    }
}
