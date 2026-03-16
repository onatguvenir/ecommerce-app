-- ============================================================
-- V1__create_notification_processed_events.sql
--
-- Idempotent Consumer Pattern tablosu.
--
-- Kafka "at-least-once" teslim garantisi verdiğinden aynı event
-- birden fazla kez tüketilebilir. Bu tablo, her notification
-- işleminin tam olarak bir kez gerçekleştirilmesini sağlar.
--
-- Benzersiz kısıt: (event_id + event_type)
-- Örneğin aynı ORDER_CREATED event'i:
--   eventId = "order-uuid-1234"
--   eventType = "ORDER_CREATED"
-- ikinci kez geldiğinde mevcut kayıt bulunur ve işlem atlanır.
-- ============================================================

CREATE TABLE IF NOT EXISTS notification_processed_events (
    id            BIGSERIAL PRIMARY KEY,
    event_id      VARCHAR(100)             NOT NULL,
    event_type    VARCHAR(100)             NOT NULL,
    processed_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_event_id_type UNIQUE (event_id, event_type)
);

-- Sorgularda kullanılacak composite index (existsByEventIdAndEventType)
CREATE INDEX IF NOT EXISTS idx_processed_events_lookup
    ON notification_processed_events (event_id, event_type);

-- TTL-benzeri temizlik için tarih üzerinden index
CREATE INDEX IF NOT EXISTS idx_processed_events_date
    ON notification_processed_events (processed_at);
