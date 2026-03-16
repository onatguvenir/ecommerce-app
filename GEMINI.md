# Monat E-Commerce Project Guide for Gemini

Hemen adaptasyon sağlaman ve maliyetli işlemleri engellemen için aşağıdaki kuralları ve mimariyi göz önünde bulundur. Bu bilgiler, proje genelinde hata yapma oranını düşürecektir.

## 1. Mimariye Genel Bakış
Monat E-Commerce, **Event-Driven**, **Microservices** tabanlı, **Spring Boot 3.x** ve **JDK 21** mimarisine sahip bir e-ticaret uygulamasıdır. Virtual Threads etkindir.

## 2. Port ve Servis Haritası (Çakışma Önleyici)
Projeye yeni bir servis veya özellik eklerken bu listeyi referans al.

### Altyapı (Infrastructure)
- **PostgreSQL**: 5432
- **MongoDB**: 27017
- **Redis**: 6379 
- **Zookeeper**: 2181
- **Kafka**: 9092
- **AKHQ (Kafka UI)**: 9000
- **RedisInsight (Redis UI)**: 8001
- **Prometheus**: 9090
- **Grafana**: 3000
- **Jaeger**: 16686 (UI)
- **ELK Stack**: 9200 (ES), 5044 (Logstash), 5601 (Kibana)

### Mikroservis Bağlantıları
| Servis Adı | HTTP Portu | gRPC Portu | DB / Cache / Event |
|---|---|---|---|
| **api-gateway** | `8080` | - | Redis (Rate limiting) |
| **user-service** | `8081` | `9081` | PostgreSQL |
| **product-service**| `8082` | - | MongoDB, Elastic, Redis |
| **inventory-service**| `8083`| `9083` | PostgreSQL, Redis |
| **cart-service** | `8084` | - | Redis |
| **order-service** | `8085` | - | PostgreSQL, Kafka (Outbox) |
| **payment-service**| `8086` | `9086` | PostgreSQL, Kafka (Outbox)|
| **notification-service**| `8087`|- | Kafka (Listener) |

## 3. Kodlama Standartları ve Prensipleri
Sistem sağlığını korumak ve "clean code" standartlarına uymak için aşağıdaki kurallar zorunludur:

- **SOLID & Clean Code**: Nesne yönelimli tasarım ilkelerine uy, "pure function" ve "immutability" (değişmezlik) kavramlarını ön planda tut. DTO'lar için her zaman `Record` kullan.
- **Concurrency (Virtual Threads)**: Proje `spring.threads.virtual.enabled=true` kullanır. I/O-bound işleri engellemeyecek asenkron/virtüel uyumlu yapılar tasarla.
- **Güvenlik (Locking)**: Finansal kayıtlar, sipariş ve stok gibi eşzamanlılıktan etkilenecek DB süreçlerinde mutlaka **Pessimistic Write Lock** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) uygula.
- **Güvenilir Mesajlaşma (Outbox)**: Kafka'ya doğrudan `KafkaTemplate.send()` çağrısı yapma. Çift yazma (dual-write) hatalarından kaçınmak için mesajları aynı DB transaction'ında `outbox_events` tablosuna sakla ve ayrı bir poller (scheduler) ile gönderim sağla (**Transactional Outbox Pattern**).
- **Defensive Programming**: NPE (NullPointerException) önlemek için `Optional`, Guard Clauses ve Spring Validator anotasyonlarını (`@NotNull`, vs.) kullan.

## 4. Analiz ve Doğrulama
Kod geliştirmesi sonrasında mutlaka ilgili test sınıflarını (Unit, Integration) oluştur veya güncelle. "Implementation Plan" sunduğun durumlarda manual test senaryolarını da mutlaka ekle.
