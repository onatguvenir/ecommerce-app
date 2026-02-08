package com.monat.ecommerce.order.domain.repository;

import com.monat.ecommerce.order.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);

    long countByProcessedFalse();
}
