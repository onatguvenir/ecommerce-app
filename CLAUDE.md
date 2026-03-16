# Monat E-Commerce Project Guide

## 1. Mimariye Genel Bakış
Monat E-Commerce Event-Driven, Microservices tabanlı, Spring Boot 3.x ve JDK 21 mimarisine sahip bir projedir.

## 2. Port ve Servis Haritası
Herhangi bir **Port Çakışması (Conflict) YÖKTÜR**. Tüm HTTP ve gRPC portları benzersiz atanmıştır:

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

### Mikroservisler
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
Aşağıdaki kurallar Agent prompt maliyetlerini düşürmek ve sistem sağlığını korumak için sıkı bir şekilde uygulanmalıdır:
- **Teknoloji**: Java 21, Spring Boot 3.x, Virtual Threads (`spring.threads.virtual.enabled=true`).
- **Idempotency & Concurrency**: Finansal ve kritik veritabanı işlemlerinde `@Lock(LockModeType.PESSIMISTIC_WRITE)` kullanılmalıdır.  Gerekmediği durumlarda (sadece update) JPA `@Version` optimistic kilidi standarttır.
- **Event-Driven İletişimi**: `KafkaTemplate` ile _doğrudan_ veri aktarımı yasaktır. Çift yazma (dual-write) problemini çözmek için **Transactional Outbox Pattern** ve `@Scheduled` tablolar kullanılacaktır.
- **Null Safety**: Hata fırlatmalar izolasyonlu (Guard Clauses, `Optional`, `@NotNull`), nesneler mümkünse **Immutable** veya `Record` olmalıdır. 

## 4. Kullanışlı Komutlar
- Sadece `payment-service` build: `mvn compile -pl payment-service --also-make -q`
- Tüm Docker mimarisini kurma: `docker compose up -d`
- Log inceleme: `docker compose logs -f [service_name]`
