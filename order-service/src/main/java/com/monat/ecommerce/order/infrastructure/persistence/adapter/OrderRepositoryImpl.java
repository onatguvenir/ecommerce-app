package com.monat.ecommerce.order.infrastructure.persistence.adapter;

import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderStatus;
import com.monat.ecommerce.order.domain.repository.OrderRepository;
import com.monat.ecommerce.order.infrastructure.persistence.entity.OrderEntity;
import com.monat.ecommerce.order.infrastructure.persistence.mapper.OrderMapper;
import com.monat.ecommerce.order.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId, int page, int size) {
        return jpaRepository.findByUserId(userId, PageRequest.of(page, size))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status, int page, int size) {
        return jpaRepository.findByStatus(status, PageRequest.of(page, size))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Order> findByIdWithItems(UUID id) {
        return jpaRepository.findByIdWithItems(id).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public long countByStatus(OrderStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public List<Order> findPendingOrdersOlderThan(LocalDateTime cutoffTime) {
        return jpaRepository.findPendingOrdersOlderThan(cutoffTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Order order) {
        jpaRepository.deleteById(order.getId());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
