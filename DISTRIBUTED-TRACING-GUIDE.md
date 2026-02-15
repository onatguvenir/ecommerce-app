# Distributed Tracing Configuration Guide

## Trace ID ve Span ID Nedir?

### Trace ID
- Bir isteğin tüm mikroservisler boyunca takip edilmesini sağlayan benzersiz kimlik
- Örnek: `64f8a7b2c3d4e5f6a7b8c9d0e1f2a3b4`
- Bir kullanıcı isteği başladığında oluşturulur ve tüm servislerde aynı kalır

### Span ID
- Bir trace içindeki her işlem için benzersiz kimlik
- Her servis kendi span ID'sini oluşturur
- Örnek: `a1b2c3d4e5f6a7b8`

### Parent Span ID
- Mevcut span'i tetikleyen üst span'in ID'si
- Servisler arası çağrı zincirini gösterir

## Log Formatı

### Konsol Çıktısı (Development)
```
2026-02-08 21:05:00 [64f8a7b2c3d4e5f6,a1b2c3d4e5f6a7b8] - Order created successfully
                     ^trace-id^         ^span-id^
```

### JSON Çıktısı (Production/ELK)
```json
{
  "timestamp": "2026-02-08T21:05:00.123Z",
  "level": "INFO",
  "service": "order-service",
  "trace_id": "64f8a7b2c3d4e5f6a7b8c9d0e1f2a3b4",
  "span_id": "a1b2c3d4e5f6a7b8",
  "parent_span_id": "b2c3d4e5f6a7b8c9",
  "thread": "http-nio-8085-exec-1",
  "logger": "com.monat.ecommerce.order.service.OrderService",
  "message": "Order created successfully",
  "stack_trace": null
}
```

## Kullanım Senaryoları

### 1. Hata Ayıklama (Debugging)

Bir kullanıcı sipariş oluştururken hata alıyor:

```bash
# Kibana'da trace ID ile arama
trace_id: "64f8a7b2c3d4e5f6a7b8c9d0e1f2a3b4"
```

**Sonuç:** Tüm servislerdeki (user, cart, order, payment) ilgili logları görebilirsiniz:
1. `user-service`: Kullanıcı doğrulandı
2. `cart-service`: Sepet getirildi
3. `order-service`: Sipariş oluşturuldu
4. `payment-service`: Ödeme başarısız! ❌

### 2. Performans Analizi

Hangi servis yavaş?

```bash
# Her servisin span'ine bakarak süreleri karşılaştırın
trace_id: "64f8a7b2c3d4e5f6" AND message: *duration*
```

### 3. Servisler Arası İlişki

Bir isteğin hangi servislere gittiğini görün:

```bash
# Zipkin UI'da trace görselleştirmesi
http://localhost:9411/zipkin/?lookback=15m&limit=10
```

## Konfigürasyon

### application.yml
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # %100 sampling (production'da 0.1 yapın)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{traceId:-},%X{spanId:-}] - %msg%n"
```

### logback-spring.xml
```xml
<pattern>
  %d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n
</pattern>
```

## Zipkin UI

### Erişim
- URL: http://localhost:9411
- Trace'leri görselleştirin
- Servis bağımlılıklarını görün
- Performans darboğazlarını tespit edin

### Özellikler
- ✅ Trace timeline görünümü
- ✅ Servis dependency grafiği
- ✅ Latency analizi
- ✅ Error tracking

## ELK ile Entegrasyon

### Kibana'da Arama

**Belirli bir trace'i bulma:**
```kql
trace_id: "64f8a7b2c3d4e5f6a7b8c9d0e1f2a3b4"
```

**Tüm hatalı trace'ler:**
```kql
log_level: "ERROR" AND trace_id: *
```

**Belirli bir kullanıcının tüm işlemleri:**
```kql
message: *userId=550e8400* AND trace_id: *
```

### Dashboard Oluşturma

1. **Trace Count by Service**
   - Visualization: Bar chart
   - X-axis: service
   - Y-axis: Count of unique trace_id

2. **Error Traces**
   - Visualization: Data table
   - Columns: timestamp, service, trace_id, message
   - Filter: log_level: ERROR

3. **Trace Duration**
   - Visualization: Line chart
   - X-axis: timestamp
   - Y-axis: duration
   - Split by: service

## Kod Örnekleri

### Manuel Span Oluşturma

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final Tracer tracer;
    
    public Order createOrder(CreateOrderRequest request) {
        // Yeni span oluştur
        Span span = tracer.nextSpan().name("create-order").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // İş mantığı
            log.info("Creating order for user: {}", request.getUserId());
            
            // Custom tag ekle
            span.tag("user.id", request.getUserId().toString());
            span.tag("order.items.count", String.valueOf(request.getItems().size()));
            
            Order order = processOrder(request);
            
            return order;
        } catch (Exception e) {
            span.tag("error", "true");
            span.tag("error.message", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Trace ID'yi Response Header'a Ekleme

```java
@Component
public class TraceIdFilter implements Filter {
    
    private final Tracer tracer;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        TraceContext context = tracer.currentSpan().context();
        httpResponse.setHeader("X-Trace-Id", context.traceId());
        httpResponse.setHeader("X-Span-Id", context.spanId());
        
        chain.doFilter(request, response);
    }
}
```

### Feign Client ile Trace Propagation

Trace ID otomatik olarak HTTP header'larında iletilir:
```
X-B3-TraceId: 64f8a7b2c3d4e5f6a7b8c9d0e1f2a3b4
X-B3-SpanId: a1b2c3d4e5f6a7b8
X-B3-ParentSpanId: b2c3d4e5f6a7b8c9
```

## Best Practices

### 1. Sampling Rate
```yaml
# Development: %100
management.tracing.sampling.probability: 1.0

# Production: %10 (yüksek trafikte)
management.tracing.sampling.probability: 0.1
```

### 2. Custom Tags
```java
span.tag("business.metric", "value");
span.tag("user.type", "premium");
span.tag("order.total", "1299.99");
```

### 3. Baggage (Context Propagation)
```java
// Baggage ekle (tüm span'lerde erişilebilir)
BaggageField userId = BaggageField.create("userId");
userId.updateValue("550e8400-e29b-41d4-a716-446655440000");

// Başka bir serviste oku
String userId = BaggageField.getByName("userId").getValue();
```

### 4. Error Handling
```java
try {
    // İşlem
} catch (Exception e) {
    span.tag("error", "true");
    span.tag("error.type", e.getClass().getSimpleName());
    span.tag("error.message", e.getMessage());
    span.error(e);
    throw e;
}
```

## Sorun Giderme

### Trace ID Görünmüyor

1. **Dependency kontrolü:**
   ```bash
   mvn dependency:tree | grep micrometer-tracing
   ```

2. **Logback konfigürasyonu:**
   ```xml
   <!-- logback-spring.xml'de %X{traceId} olmalı -->
   <pattern>%d [%X{traceId:-},%X{spanId:-}] %msg%n</pattern>
   ```

3. **Application properties:**
   ```yaml
   management.tracing.sampling.probability: 1.0
   ```

### Zipkin'e Trace Gönderilmiyor

1. **Zipkin çalışıyor mu?**
   ```bash
   curl http://localhost:9411/health
   ```

2. **Endpoint doğru mu?**
   ```yaml
   management.zipkin.tracing.endpoint: http://localhost:9411/api/v2/spans
   ```

3. **Network bağlantısı:**
   ```bash
   docker-compose logs zipkin
   ```

## Faydalı Linkler

- **Micrometer Tracing:** https://micrometer.io/docs/tracing
- **Zipkin:** https://zipkin.io/
- **Spring Boot Tracing:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing

## Özet

✅ **Trace ID**: İsteği tüm servislerde takip et
✅ **Span ID**: Her servisin işlemini izole et  
✅ **Parent Span ID**: Servis çağrı zincirini gör
✅ **Zipkin**: Görsel trace analizi
✅ **ELK**: Log korelasyonu ve arama
✅ **Custom Tags**: İş metriklerini ekle

**Sonuç:** Mikroservislerinizde tam görünürlük! 🎯
