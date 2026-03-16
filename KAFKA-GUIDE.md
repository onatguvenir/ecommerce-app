# Kafka Kullanım Özeti

Proje, mikroservisler arası **asenkron event-driven iletişim** için Apache Kafka kullanmaktadır.

## Genel Mimari

```
order-service ──(Outbox Pattern)──► Kafka ──► notification-service
                                      │
payment-service ─(direkt KafkaTemplate)► Kafka ──► notification-service
```

---

## Topic Envanteri

| Topic | Publisher | Consumer | Amaç |
|-------|-----------|----------|------|
| `order.created` | order-service | notification-service | Sipariş oluşturuldu bildirimi |
| `order.completed` | order-service | notification-service | Sipariş tamamlandı bildirimi |
| `order.cancelled` | order-service | notification-service | Sipariş iptal bildirimi |
| `order.events` | order-service | — | Sayılamayan order event'leri |
| `payment.completed` | payment-service | notification-service | Ödeme başarılı bildirimi |
| `payment.failed` | payment-service | notification-service | Ödeme başarısız bildirimi |

---

## Ortak Konfigürasyon — `common-lib`

**[`KafkaCommonConfig.java`](file:///c:/Users/Monat/Desktop/Projects/monat-ecommerce/common-lib/src/main/java/com/monat/ecommerce/common/config/KafkaCommonConfig.java)**

Tüm producer'lar tarafından paylaşılan `KafkaTemplate<String, Object>` bean'ini tanımlar.

```java
// Temel producer özellikleri:
ACKS_CONFIG     = "all"       // Güvenli teslim: lider + tüm replica'lar onaylamalı
RETRIES_CONFIG  = 3           // Hata durumunda 3 kez yeniden dene
ENABLE_IDEMPOTENCE = true     // Aynı mesajın birden fazla kez yazılmasını önler
KEY_SERIALIZER  = StringSerializer    // Partition key: orderId / paymentId
VALUE_SERIALIZER = JsonSerializer     // Payload: JSON
ADD_TYPE_INFO_HEADERS = false // Tip bilgisi header'a eklenmez (consumer uyumluluğu için)
```

---

## Producers

### 1. order-service — Outbox Pattern

**[`OutboxEventPublisher.java`](file:///c:/Users/Monat/Desktop/Projects/monat-ecommerce/order-service/src/main/java/com/monat/ecommerce/order/infrastructure/messaging/OutboxEventPublisher.java)**

> **Neden Outbox?** Sipariş kaydı ve Kafka mesajı tek bir atomik transaction içinde yapılmalıdır. Önce DB'ye `OutboxEvent` yazılır; ayrı bir `@Scheduled` poller bunu Kafka'ya iletir. Böylece "mesaj gönderildi ama DB kaydı olmadı" veya "DB yazıldı ama Kafka'ya ulaşmadı" senaryoları engellenir.

```
[Order Transaction]
    ├── orders tablosuna yaz
    └── outbox_events tablosuna yaz (processed=false)

[OutboxEventPublisher — her 5 sn @Scheduled]
    ├── processed=false olan eventleri al (batch: 100)
    ├── KafkaTemplate.send(topic, aggregateId, payload)
    └── event.processed = true olarak güncelle
```

**Topic mapping:**
```java
"OrderCreated"   → "order.created"
"OrderCompleted" → "order.completed"
"OrderCancelled" → "order.cancelled"
default          → "order.events"
```

---

### 2. payment-service — Direkt KafkaTemplate

**[`PaymentDomainService.java`](file:///c:/Users/Monat/Desktop/Projects/monat-ecommerce/payment-service/src/main/java/com/monat/ecommerce/payment/domain/service/PaymentDomainService.java)**

Outbox pattern kullanmadan, ödeme işlemi sonrasında `KafkaTemplate.send()` ile direkt publish eder.
Partition key olarak `orderId` kullanılır — aynı siparişe ait tüm eventler aynı partition'a gider.

```
Ödeme başarılı → kafkaTemplate.send("payment.completed", orderId, PaymentCompletedEvent)
Ödeme başarısız → kafkaTemplate.send("payment.failed",    orderId, PaymentFailedEvent)
```

> [!WARNING]
> payment-service Outbox pattern kullanmıyor. Servis çöker ve DB transaction rollback olursa, başarılı ödeme kaydı düşer ama Kafka'ya mesaj ulaşmaz. İleride Outbox pattern'e geçiş önerilir.

---

## Consumers

### notification-service

Consumer group: `notification-service-group`

**[`OrderEventConsumer.java`](file:///c:/Users/Monat/Desktop/Projects/monat-ecommerce/notification-service/src/main/java/com/monat/ecommerce/notification/infrastructure/messaging/OrderEventConsumer.java)** — 3 listener:

| Annotation | Event |
|------------|-------|
| `@KafkaListener(topics = "order.created")` | `OrderCreatedEvent` |
| `@KafkaListener(topics = "order.completed")` | `OrderCompletedEvent` |
| `@KafkaListener(topics = "order.cancelled")` | `OrderCancelledEvent` |

**[`PaymentEventConsumer.java`](file:///c:/Users/Monat/Desktop/Projects/monat-ecommerce/notification-service/src/main/java/com/monat/ecommerce/notification/infrastructure/messaging/PaymentEventConsumer.java)** — 2 listener:

| Annotation | Event |
|------------|-------|
| `@KafkaListener(topics = "payment.completed")` | `PaymentCompletedEvent` |
| `@KafkaListener(topics = "payment.failed")` | `PaymentFailedEvent` |

---

## application.yml Konfigürasyonları

### order-service, payment-service, inventory-service (Producer)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
```

### order-service (ayrıca Consumer)
```yaml
    consumer:
      group-id: order-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: '*'
```

### notification-service (Sadece Consumer)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

---

## Eksikler / İyileştirme Önerileri

| # | Konu | Açıklama |
|---|------|----------|
| 1 | payment-service Outbox | Direkt KafkaTemplate kullanımı yerine Outbox pattern eklenmeli |
| 2 | Dead Letter Topic (DLT) | Consumer hataları için DLT konfigürasyonu yok |
| 3 | Schema Registry | Event şemaları Avro/Protobuf ile yönetilmiyor; JSON type-unsafe |
| 4 | Topic'lerin programatik oluşturulması | Topic'ler için `NewTopic` bean tanımı yok (manuel oluşturma gerekiyor) |
| 5 | Idempotency on consumer | Consumer tarafında duplicate event koruması yok |
