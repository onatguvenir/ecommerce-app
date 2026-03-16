package com.monat.ecommerce.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Idempotent Consumer tablosu.
 *
 * Kafka "at-least-once" sağlar, yani ağ problemi sonrasında aynı event
 * birden fazla kez gelebilir. Bu entity, her event'in yalnızca bir kez
 * işlendiğini garanti ederek müşterilere çift bildirim gitmesin diye
 * bir "iz" bırakır.
 *
 * Benzersiz anahtar stratejisi: eventId + eventType kombinasyonu
 *   - eventId   → Kafka mesajının UUID benzeri kimliği (genellikle orderId veya paymentId)
 *   - eventType → "ORDER_CREATED", "PAYMENT_COMPLETED" vb.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "notification_processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_id_type",
                columnNames = {"event_id", "event_type"}
        )
)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Event'in benzersiz kimliği (orderId, paymentId gibi).
     * Consumer'lar bu değeri mesajdan alarak kontrol eder.
     */
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    /**
     * Event tipi: "ORDER_CREATED", "ORDER_COMPLETED", "PAYMENT_COMPLETED" vb.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * İşlem tamamlandığı zaman — TTL/cleanup politikaları için kullanılabilir.
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /**
     * Factory metodu — immutable oluşturma için.
     */
    public static ProcessedEvent of(String eventId, String eventType) {
        ProcessedEvent e = new ProcessedEvent();
        e.eventId = eventId;
        e.eventType = eventType;
        e.processedAt = LocalDateTime.now();
        return e;
    }
}
