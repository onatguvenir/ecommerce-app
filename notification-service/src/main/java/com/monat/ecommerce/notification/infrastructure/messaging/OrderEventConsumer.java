package com.monat.ecommerce.notification.infrastructure.messaging;

import com.monat.ecommerce.events.order.OrderCancelledEvent;
import com.monat.ecommerce.events.order.OrderCompletedEvent;
import com.monat.ecommerce.events.order.OrderCreatedEvent;
import com.monat.ecommerce.grpc.user.User;
import com.monat.ecommerce.notification.domain.model.ProcessedEvent;
import com.monat.ecommerce.notification.domain.service.EmailService;
import com.monat.ecommerce.notification.domain.service.SmsService;
import com.monat.ecommerce.notification.infrastructure.grpc.UserServiceClient;
import com.monat.ecommerce.notification.infrastructure.persistence.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Sipariş event'lerini tüketen Kafka Consumer.
 *
 * Production-Grade özellikler:
 *
 * 1. Idempotency — Her event işlenmeden önce ProcessedEvent tablosunda
 *    varlığı kontrol edilir. Aynı mesaj iki kez gelse bile ikinci seferinde
 *    işlem atlanır.
 *
 * 2. gRPC User Service Entegrasyonu — Event içindeki userId ile gerçek
 *    müşteri bilgisi (e-posta, isim) çekilir; CircuitBreaker korumalıdır.
 *
 * 3. DLQ (Dead Letter Queue) — Exception'lar catch bloğu yerine dışarı
 *    fırlatılır. KafkaConsumerConfig içindeki DefaultErrorHandler gerekli
 *    retry'ları yapar ve başarısız mesajı <topic>.DLT'ye taşır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;
    private final ProcessedEventRepository processedEventRepository;

    // -------------------------------------------------------------------------
    // ORDER CREATED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.created", groupId = "notification-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_CREATED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: orderId={}, type={}", eventKey, eventType);
            return;
        }

        log.info("Received OrderCreatedEvent: orderId={}, orderNumber={}", event.getOrderId(), event.getOrderNumber());

        Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            emailService.sendOrderConfirmation(
                    user.getEmail(),
                    event.getOrderNumber(),
                    user.getFirstName() + " " + user.getLastName(),
                    event.getTotalAmount().toString()
            );
            log.info("Order confirmation email sent: orderId={}, email={}", eventKey, user.getEmail());
        } else {
            log.warn("Kullanıcı bilgisi alınamadı, bildirim atlandı: userId={}", event.getUserId());
        }

        markAsProcessed(eventKey, eventType);
    }

    // -------------------------------------------------------------------------
    // ORDER COMPLETED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.completed", groupId = "notification-service-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_COMPLETED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: orderId={}, type={}", eventKey, eventType);
            return;
        }

        log.info("Received OrderCompletedEvent: orderId={}", event.getOrderId());

        Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            emailService.sendOrderCompleted(
                    user.getEmail(),
                    event.getOrderNumber(),
                    user.getFirstName() + " " + user.getLastName()
            );
            log.info("Order completed email sent: orderId={}", eventKey);
        } else {
            log.warn("Kullanıcı bilgisi alınamadı, bildirim atlandı: userId={}", event.getUserId());
        }

        markAsProcessed(eventKey, eventType);
    }

    // -------------------------------------------------------------------------
    // ORDER CANCELLED
    // -------------------------------------------------------------------------

    @Transactional
    @KafkaListener(topics = "order.cancelled", groupId = "notification-service-group")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        String eventKey = event.getOrderId().toString();
        String eventType = "ORDER_CANCELLED";

        if (isAlreadyProcessed(eventKey, eventType)) {
            log.warn("[Idempotency] Atlanıyor: orderId={}, type={}", eventKey, eventType);
            return;
        }

        log.info("Received OrderCancelledEvent: orderId={}, reason={}", event.getOrderId(), event.getReason());

        Optional<User> userOpt = userServiceClient.getUser(event.getUserId().toString());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            emailService.sendOrderCancelled(
                    user.getEmail(),
                    event.getOrderNumber(),
                    user.getFirstName() + " " + user.getLastName(),
                    event.getReason()
            );
            log.info("Order cancelled email sent: orderId={}", eventKey);
        } else {
            log.warn("Kullanıcı bilgisi alınamadı, bildirim atlandı: userId={}", event.getUserId());
        }

        markAsProcessed(eventKey, eventType);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.existsByEventIdAndEventType(eventId, eventType);
    }

    private void markAsProcessed(String eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.of(eventId, eventType));
    }
}
