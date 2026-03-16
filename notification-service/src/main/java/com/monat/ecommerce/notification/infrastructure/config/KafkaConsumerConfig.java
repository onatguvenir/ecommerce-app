package com.monat.ecommerce.notification.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Kafka Consumer hata yönetimi konfigürasyonu.
 *
 * Üretim ortamında bir Kafka mesajı işlenirken hata oluşursa:
 *
 * 1. ExponentialBackOff → 1sn, 2sn, 4sn... aralıklarla backoff yaparak
 *    toplamda MAX_ATTEMPTS kez yeniden denenir. Bu geçici ağ veya DB
 *    sorunlarını otomatik atlatmayı sağlar.
 *
 * 2. DeadLetterPublishingRecoverer → MAX_ATTEMPTS sonrasında mesaj
 *    "<topic>.DLT" (Dead Letter Topic) topiğine taşınır.
 *    Örn: "order.created" → "order.created.DLT"
 *    DLT üzerindeki mesajlar operasyon ekibi tarafından incelenebilir
 *    veya ayrı bir DLT consumer servisi tarafından işlenebilir.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    /** Başlangıç bekleme süresi: 1 saniye */
    private static final long INITIAL_INTERVAL_MS = 1_000L;
    /** Maksimum bekleme süresi: 30 saniye */
    private static final long MAX_INTERVAL_MS = 30_000L;
    /** Toplam yeniden deneme sayısı (backoff dahil) */
    private static final long MAX_ATTEMPTS = 3L;

    /**
     * Merkezi Kafka hata handler'ı.
     *
     * @param kafkaTemplate DLT'ye mesaj yayınlamak için kullanılan şablon
     * @return Spring Kafka container'ına bağlanan hata handler'ı
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

        // Hata oluşan mesajları otomatik olarak "<topic>.DLT" topiğine taşır.
        // Varsayılan davranış: aynı partition key ile hedef topiğe yazar.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("Message failed after {} attempts. Moving to DLT. topic={}, partition={}, offset={}. Error: {}",
                            MAX_ATTEMPTS, record.topic(), record.partition(), record.offset(), ex.getMessage());
                    // Mesajı aynı anda DLT'ye yönlendiren default TopicPartition döner
                    return new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition());
                }
        );

        // Exponential Backoff: 1s → 2s → 4s → ... max 30s
        ExponentialBackOff backOff = new ExponentialBackOff(INITIAL_INTERVAL_MS, 2.0);
        backOff.setMaxElapsedTime(MAX_INTERVAL_MS * MAX_ATTEMPTS);
        backOff.setMaxInterval(MAX_INTERVAL_MS);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Tekrar denenemeyen (non-retryable) istisnalar — anında DLT'ye gider
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,   // Geçersiz mesaj formatı
                ClassCastException.class          // Deserialize sorunu
        );

        return errorHandler;
    }
}
