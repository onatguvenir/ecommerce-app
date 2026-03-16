package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.payment.PaymentCompletedEvent;
import com.monat.ecommerce.events.payment.PaymentFailedEvent;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ödeme event'lerini tüketen Kafka Consumer.
 *
 * Tasarım notu:
 *  PaymentCompletedEvent ve PaymentFailedEvent'te userId alanı bulunmaz.
 *  Bu nedenle User Service çağrısı yapılamaz. Müşteri bildirimleri esas
 *  olarak OrderEventConsumer üzerinden (order.completed, order.cancelled)
 *  gerçekleştirilir. Payment consumer'ı DLQ / Idempotency koruma altında
 *  loglama ve iç bildirim işlemleri için saklanmaktadır.
 *
 *  Production-Grade özellikler:
 *   - Idempotency koruması (ProcessedEvent tablosu)
 *   - Exception fırlatma → KafkaConsumerConfig DefaultErrorHandler devreye girer (DLQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "payment.completed", groupId = "notification-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_COMPLETED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: paymentId={}, type={}", eventKey, eventType);
            return;
        }

        log.info("Payment completed: paymentId={}, orderId={}, amount={}",
                event.getPaymentId(), event.getOrderId(), event.getAmount());

        // Müşteri bildirimi OrderEventConsumer > order.completed event'i ile gönderilir.
        // Bu consumer sadece iç işlem kaydı / audit amacıyla çalışır.

        markAsProcessed(eventKey, eventType);
    }

    @Transactional
    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        String eventKey = event.getPaymentId();
        String eventType = "PAYMENT_FAILED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: paymentId={}, type={}", eventKey, eventType);
            return;
        }

        // Ödeme başarısız — sipariş iptal event'i gelince order.cancelled consumer tetiklenecek
        log.warn("Payment failed: paymentId={}, orderId={}, reason={}",
                event.getPaymentId(), event.getOrderId(), event.getFailureReason());

        markAsProcessed(eventKey, eventType);
    }

    private boolean isAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
    }

    private void markAsProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.of(eventId, eventType));
    }
}
