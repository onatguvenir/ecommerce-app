# Test Raporu — 2026-04-18

> **Kaynak:** `docs/test-roadmap.md` senaryoları — Docker Compose ortamı (`http://localhost:8080` / `http://localhost:5173`)  
> **Durum:** 24/27 test geçti · Aşağıdaki sorunlar çözüm bekliyor

---

## İçindekiler

1. [Test Koşusu Özeti](#1-test-koşusu-özeti)
2. [Backend](#2-backend)
3. [Frontend](#3-frontend)
4. [Aksiyon Planı](#4-aksiyon-planı)

---

## 1. Test Koşusu Özeti

| Kategori | Toplam | ✅ Geçti | ⚠️ Kısmi | ❌ Başarısız |
|---|---|---|---|---|
| Kullanıcı Senaryoları (A–F) | 19 | 17 | 2 | 0 |
| Veri Tutarlılığı (VC) | 4 | 3 | 1 | 0 |
| Sistem Sağlığı (SH) | 4 | 4 | 0 | 0 |
| **Toplam** | **27** | **24** | **3** | **0** |

**Kısmi geçen testler:**
- **D-1** Ürün Listesi — API Gateway `/graphql` yönlendirmesi eksik; doğrudan 8082 ✅, gateway 8080 ❌
- **F-1** Sipariş Oluşturma — Endpoint 201 döndü ✅; Saga asenkron olarak FAILED (user-service gRPC hatası)
- **VC-3** Sipariş/Stok Tutarlılığı — Saga başarısız olduğu için stok rezervasyonu doğrulanamadı

---

## 2. Backend

### Sorun B-1 — user-service: Order Saga Her Seferinde FAILED

**Öncelik:** YÜKSEK 🔴  
**Servis:** `user-service` → `UserGrpcServiceImpl.java:58`  
**Etki:** Sipariş akışının tamamı çalışmıyor

**Hata:**
```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role:
  UserEntity.addresses: could not initialize proxy - no Session
    at UserGrpcServiceImpl.validateUser(UserGrpcServiceImpl.java:58)
```

**Kök Neden:** `validateUser` metodu `findById` çağırdıktan sonra `UserEntity.addresses` lazy koleksiyonuna JPA session dışında erişiliyor.

**Düzeltme:**
```java
@Override
@Transactional(readOnly = true)   // ← ekle
public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
```

---

### Sorun B-2 — Çoklu Servis: YAML `nplus1` Yapı Hatası

**Öncelik:** ORTA 🟡  
**Etkilenen Servisler:** `notification-service`, `user-service` (ve potansiyel olarak diğerleri)  
**Risk:** Servisler yalnızca docker-compose env var'ları sayesinde çalışıyor; env var kaldırılırsa `localhost` adreslerine bağlanır ve hata verir.

```yaml
# YANLIŞ (mevcut durum):
nplus1:
  enabled: true
  threads:       # ← spring.threads olması gerekiyor
    virtual:
      enabled: true
  datasource:    # ← spring.datasource olması gerekiyor
    url: ...

# DOĞRU:
nplus1:
  enabled: true

spring:
  threads:
    virtual:
      enabled: true
  datasource:
    url: ...
```

**Kontrol edilmesi gerekenler:** Tüm servislerin `application.yml` dosyaları.

---

### Sorun B-3 — notification-service: Maven Build Hatası

**Öncelik:** ORTA 🟡  
**Etki:** Docker image yeniden oluşturulamıyor; güncel kod deploy edilemiyor; e-posta bildirimleri çalışmıyor

```
[ERROR] UnresolvableModelException — exit code: 1
```

**Önerilen Aksiyon:** `docker compose build --no-cache notification-service`; hata devam ederse `mvn dependency:tree -pl notification-service`

---

### Sorun B-4 — payment-service: Micrometer Gauge Tekrarlayan DB Query

**Öncelik:** DÜŞÜK 🟢  
**Belirti:** Log'larda dakikada bir N+1 detector uyarısı; test süresi boyunca 291–297 tekrar sayıldı

```
ERROR QueryCaptureListener - N+1 PROBLEM DETECTED (Repetitions: 293)
  Query: select count(...) from payment_outbox_events where not(processed)
  Location: DefaultGauge.value (DefaultGauge.java:53)
```

**Kök Neden:** Micrometer gauge her scrape'te DB'ye `COUNT` sorgusu atıyor. Teknik N+1 değil — N+1 detector periyodik gauge sorgusunu yanlış alarm olarak işaretliyor.

**Düzeltme:** Gauge'u `@Scheduled` ile güncellenen `AtomicLong` ile besle:
```java
private final AtomicLong pendingCount = new AtomicLong(0);

@Scheduled(fixedDelay = 60_000)
public void refresh() {
    pendingCount.set(outboxRepository.countByProcessedFalse());
}
// Gauge.builder(..., () -> pendingCount.get())
```

---

### Yapısal Eksikler

| # | Eksik | Etki | Öncelik |
|---|---|---|---|
| S-1 | Hiçbir serviste `application-docker.yml` profil dosyası yok | Env var silindiğinde servis `localhost`'a bağlanır | 🟡 Orta |
| S-2 | SkyWalking OAP/UI exited (exit 137/143) | Distributed tracing ve APM devre dışı | 🟡 Orta |
| S-3 | Inventory seed verisi 5/20 ürün | Stok olmayan ürünler için sipariş testi yapılamıyor | 🟡 Orta |

---

## 4. Aksiyon Planı

| # | Aksiyon | Alan | Öncelik |
|---|---|---|---|
| 1 | `UserGrpcServiceImpl.validateUser`'a `@Transactional(readOnly = true)` ekle | Backend | 🔴 Yüksek |
| 2 | api-gateway'e `/graphql` route ekle, rebuild | Backend | 🔴 Yüksek |
| 4 | notification-service Maven build hatasını çöz | Backend | 🟡 Orta |
| 5 | Tüm servislerin YAML `nplus1` girintisini düzelt | Backend | 🟡 Orta |
| 6 | Her servis için `application-docker.yml` oluştur | Backend | 🟡 Orta |
| 7 | Inventory seed verisini 20 ürünü kapsayacak şekilde genişlet | Backend | 🟡 Orta |
| 8 | SkyWalking OAP/UI container'larını yeniden başlat | Backend | 🟡 Orta |
| 11 | payment-service Micrometer gauge'u önbellekle besle | Backend | 🟢 Düşük |
