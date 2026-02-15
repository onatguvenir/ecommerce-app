package com.monat.ecommerce.order.domain.repository;

import com.monat.ecommerce.order.domain.model.Order;
import com.monat.ecommerce.order.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository Interface - Defines the contract for Order persistence
 * Independent of implementation details (JPA, etc.)
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(UUID userId, int page, int size);

    List<Order> findByStatus(OrderStatus status, int page, int size);

    Optional<Order> findByIdWithItems(UUID id);

    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    long countByUserId(UUID userId);

    long countByStatus(OrderStatus status);

    long count();

    void deleteAll();

    List<Order> findPendingOrdersOlderThan(LocalDateTime cutoffTime);

    void delete(Order order);
}
