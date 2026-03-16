# Notification Service - Production-Grade İyileştirme Planı

Bu plan, `notification-service` modülünü hata toleranslı, dayanıklı ve esnek bir "Production-Grade" (Üretim Düzeyi) mimariye taşımayı amaçlayan 5 ana adımı içerir.

## 1. User Service Entegrasyonu (gRPC Client)
Müşteri e-posta ve telefon numarası gibi bilgileri Kafka event'i üzerinden değil, güvenli bir şekilde `user-service` üzerinden çekilmelidir.
- [NEW] `infrastructure/grpc/UserServiceClient.java` sınıfı eklenecek.
- [MODIFY] `application.yml` dosyasına User Service için gRPC port (9081) bilgisi eklenecek.
- [MODIFY] `pom.xml` dosyasına Resilience4j ve grpc-server-spring-boot-starter (client için de kullanılır) eklenecek.
- [MODIFY] `OrderEventConsumer` ve `PaymentEventConsumer` sınıflarındaki `#TODO` kısımları kaldırılarak gerçek müşteri verisi `UserServiceClient` ile çekilecek.

## 2. Mail Şablon Motoru (Freemarker) ve Çoklu Dil Desteği (i18n)
Maillerin hard-coded Java String'leri yerine profesyonel HTML şablonları üzerinden gönderilmesi sağlanacaktır.
- [MODIFY] `pom.xml` ortamına `spring-boot-starter-freemarker` eklenecek.
- [NEW] `src/main/resources/templates/` altına `order-created.ftl` ve `payment-completed.ftl` gibi HTML şablon dosyaları eklenecek.
- [MODIFY] `EmailService`, Freemarker `#Template` motoruyla çalışacak şekilde (MimeMessageHelper ile) refactor edilecek.

## 3. Kafka Dead Letter Queue (DLQ) ve Retry Mekanizması
Hata alan veya User Service'e anlık ulaşılamamasından kaynaklı işlenemeyen event'lerin kaybolmasını engellemek için Kafka exception handling kurgulanacaktır.
- [NEW] `infrastructure/config/KafkaConsumerConfig.java` oluşturularak `DefaultErrorHandler` ve `DeadLetterPublishingRecoverer` (örneğin 3 deneme sonrası `.DLT` topiğine atma) konfigürasyonu yapılacak.
- [MODIFY] `OrderEventConsumer` ve `PaymentEventConsumer` içindeki genel "catch (Exception)" blokları kaldırılarak veya daraltılarak hataların Spring Kafka container'ına (ve dolayısıyla DLQ'ya) fırlatılması sağlanacak.

## 4. Idempotency (Aynı Mesajı Sadece Bir Kere İşleme)
Kafka "at-least-once" çalışır. Aynı event'in birden fazla kez okunup aynı mailin atılmaması için veritabanında (Redis veya Postgres) küçük bir iz (lock/set) bırakılacaktır.
- *Mimari Seçim:* Projede halihazırda PostgreSQL var; güvenilir transactional kontrol için bir JPA Tablosu kullanılabilir (`notification_idempotency_keys`).
- [NEW] `domain/model/ProcessedEvent.java` (Entity tablosu) tanımlanacak.
- [MODIFY] Consumer sınıfları, mesajı işlemeden önce `eventId` veya eşsiz `orderId+status` anahtarıyla bu tabloda sorgulama yapacak, varsa işlemi atlayacak.

## 5. Sanal İş Parçacıkları (Virtual Threads) & Async Gönderim
I/O bound olan E-Mail API ve SMS API gönderimleri Spring boot Virtual Thread'leri ile asenkron yürütülecektir.
- [MODIFY] `EmailService` ve `SmsService` metotlarına `@Async` eklenecek.
- [NEW] `infrastructure/config/AsyncConfig.java` ile `@EnableAsync` aktif edilip thread-pool'un virtual thread kullanması garanti edilecek.
