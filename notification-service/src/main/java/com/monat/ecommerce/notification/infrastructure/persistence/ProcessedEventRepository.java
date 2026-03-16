package com.monat.ecommerce.notification.infrastructure.persistence;

import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ProcessedEvent repository.
 *
 * existsByEventIdAndEventType: Consumer'ların idempotency kontrolü için
 * kullanacağı temel sorgu. Database'de unique constraint sayesinde
 * eş zamanlı çakışmalar da engellenmiş olur.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Verilen eventId + eventType kombinasyonunun daha önce işlenip işlenmediğini kontrol eder.
     */
    boolean existsByEventIdAndEventType(String eventId, String eventType);
}
