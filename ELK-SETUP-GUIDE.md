# ELK Stack Kurulum ve Kullanım Kılavuzu

## 📋 İçindekiler
- [Kurulum](#kurulum)
- [Kibana'ya Erişim](#kibanaya-erişim)
- [Log Arama ve Filtreleme](#log-arama-ve-filtreleme)
- [Dashboard Oluşturma](#dashboard-oluşturma)
- [Sorun Giderme](#sorun-giderme)

---

## 🚀 Kurulum

### 1. ELK Stack'i Başlatma

```bash
# Tüm servisleri başlat (ELK dahil)
docker-compose up -d

# Sadece ELK stack'i başlat
docker-compose up -d elasticsearch logstash kibana filebeat

# Servislerin durumunu kontrol et
docker-compose ps
```

### 2. Servislerin Hazır Olmasını Bekleyin

ELK stack'in tamamen başlaması 2-3 dakika sürebilir:

```bash
# Elasticsearch'ün hazır olup olmadığını kontrol et
curl http://localhost:9200/_cluster/health

# Kibana'nın hazır olup olmadığını kontrol et
curl http://localhost:5601/api/status

# Logları izle
docker-compose logs -f elasticsearch logstash kibana
```

---

## 🌐 Kibana'ya Erişim

### Kibana Web UI

**URL:** http://localhost:5601

İlk açılışta Kibana'nın yüklenmesi birkaç dakika sürebilir.

---

## 🔍 Log Arama ve Filtreleme

### Adım 1: Index Pattern Oluşturma

1. Kibana'ya gidin: http://localhost:5601
2. Sol menüden **Stack Management** → **Index Patterns** seçin
3. **Create index pattern** butonuna tıklayın
4. Index pattern: `microservices-logs-*` yazın
5. **Next step** butonuna tıklayın
6. Time field: `@timestamp` seçin
7. **Create index pattern** butonuna tıklayın

### Adım 2: Discover'da Log Görüntüleme

1. Sol menüden **Discover** seçin
2. Sağ üst köşeden zaman aralığını seçin (örn: Last 15 minutes)
3. Loglarınız görünecektir

### Adım 3: Filtreleme ve Arama

#### Basit Arama
```
# Belirli bir kelime ara
ERROR

# Birden fazla kelime
Exception OR Error

# Tam eşleşme
"Order created successfully"
```

#### KQL (Kibana Query Language) ile Gelişmiş Arama

```kql
# Belirli bir servisten loglar
container.name: "user-service"

# Belirli log seviyesi
log_level: "ERROR"

# Birden fazla servis
container.name: ("user-service" OR "order-service")

# Zaman aralığı ve servis
container.name: "order-service" AND log_level: "ERROR"

# Wildcard kullanımı
message: *Exception*

# Belirli bir kullanıcı ID'si
message: *userId=123*
```

#### Lucene Query ile Arama

```lucene
# HTTP 500 hataları
message:500 AND log_level:ERROR

# Belirli bir endpoint
message:/api/orders* AND container.name:order-service

# NOT operatörü
container.name:*-service NOT container.name:notification-service

# Range query
@timestamp:[now-1h TO now]
```

### Adım 4: Filtreleri Kaydetme

1. Arama yaptıktan sonra sağ üst köşeden **Save** butonuna tıklayın
2. Filtreye bir isim verin (örn: "Order Service Errors")
3. **Save** butonuna tıklayın

---

## 📊 Dashboard Oluşturma

### Örnek Dashboard: Mikroservis Logları

#### 1. Visualization Oluşturma

**Error Count by Service:**
1. **Visualize** → **Create visualization** → **Vertical Bar**
2. Index pattern: `microservices-logs-*` seçin
3. Y-axis: Count
4. X-axis: Terms → `container.name.keyword`
5. Filter: `log_level: ERROR`
6. **Save** → "Errors by Service"

**Log Level Distribution:**
1. **Visualize** → **Create visualization** → **Pie Chart**
2. Index pattern: `microservices-logs-*` seçin
3. Slice by: Terms → `log_level.keyword`
4. **Save** → "Log Level Distribution"

**Logs Over Time:**
1. **Visualize** → **Create visualization** → **Line**
2. Index pattern: `microservices-logs-*` seçin
3. Y-axis: Count
4. X-axis: Date Histogram → `@timestamp`
5. Split series: Terms → `container.name.keyword`
6. **Save** → "Logs Over Time"

#### 2. Dashboard Oluşturma

1. **Dashboard** → **Create dashboard**
2. **Add** butonuna tıklayın
3. Oluşturduğunuz visualizationları ekleyin
4. **Save** → "Microservices Monitoring"

---

## 🎯 Kullanışlı Sorgular

### Hata Logları
```kql
log_level: "ERROR" OR log_level: "WARN"
```

### Belirli Bir Endpoint
```kql
message: "/api/orders" AND container.name: "order-service"
```

### Exception Stack Trace
```kql
message: *Exception* OR message: *Error*
```

### Yavaş İstekler (örn: 1 saniyeden uzun)
```kql
message: *duration* AND message: *ms* AND NOT message: *[0-9][0-9]ms*
```

### Belirli Bir Kullanıcının İşlemleri
```kql
message: *userId=550e8400-e29b-41d4-a716-446655440000*
```

### HTTP Status Kodları
```kql
# 4xx hataları
message: *status=4*

# 5xx hataları
message: *status=5*
```

---

## 🔧 Gelişmiş Özellikler

### 1. Alert Oluşturma

Kibana'da belirli koşullarda alert oluşturabilirsiniz:

1. **Stack Management** → **Rules and Connectors**
2. **Create rule** butonuna tıklayın
3. Rule type: **Elasticsearch query**
4. Index: `microservices-logs-*`
5. Query: `log_level: "ERROR"`
6. Threshold: Count > 10 in 5 minutes
7. Action: Email, Slack, vb.

### 2. Saved Searches

Sık kullandığınız aramaları kaydedin:

1. Discover'da arama yapın
2. Sağ üst köşeden **Save** butonuna tıklayın
3. İsim verin ve kaydedin

### 3. Field Filtering

Sol taraftaki field listesinden ilgilendiğiniz fieldları seçin:
- `container.name`
- `log_level`
- `message`
- `@timestamp`
- `service_name`

---

## 📈 Performans İzleme

### Elasticsearch Cluster Health
```bash
curl http://localhost:9200/_cluster/health?pretty
```

### Index Boyutları
```bash
curl http://localhost:9200/_cat/indices?v
```

### Logstash Pipeline Stats
```bash
curl http://localhost:9600/_node/stats?pretty
```

---

## 🛠️ Sorun Giderme

### Elasticsearch Başlamıyor

```bash
# Logları kontrol et
docker-compose logs elasticsearch

# Container'ı yeniden başlat
docker-compose restart elasticsearch

# Disk alanını kontrol et
docker system df
```

**Yaygın Hatalar:**

1. **"max virtual memory areas vm.max_map_count [65530] is too low"**
   ```bash
   # Windows WSL2'de:
   wsl -d docker-desktop
   sysctl -w vm.max_map_count=262144
   exit
   ```

2. **"Elasticsearch is not ready"**
   - Elasticsearch'ün başlaması 1-2 dakika sürebilir
   - Health check'i kontrol edin: `curl http://localhost:9200/_cluster/health`

### Kibana'ya Erişilemiyor

```bash
# Kibana loglarını kontrol et
docker-compose logs kibana

# Elasticsearch'ün çalıştığından emin olun
curl http://localhost:9200

# Kibana'yı yeniden başlat
docker-compose restart kibana
```

### Loglar Görünmüyor

```bash
# Filebeat loglarını kontrol et
docker-compose logs filebeat

# Logstash loglarını kontrol et
docker-compose logs logstash

# Index'leri kontrol et
curl http://localhost:9200/_cat/indices?v

# Filebeat'i yeniden başlat
docker-compose restart filebeat
```

### Index Pattern Oluşturulamıyor

1. Elasticsearch'te index olduğundan emin olun:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

2. `microservices-logs-*` pattern'ine uyan index varsa:
   - Kibana'da Index Patterns'e gidin
   - Pattern'i tekrar oluşturmayı deneyin

### Disk Doldu

```bash
# Eski index'leri sil (30 günden eski)
curl -X DELETE "http://localhost:9200/microservices-logs-2024.01.*"

# Tüm logları temizle (DİKKAT!)
curl -X DELETE "http://localhost:9200/microservices-logs-*"

# Docker volume'leri temizle
docker system prune -a --volumes
```

---

## 📝 Log Retention Policy

Logları otomatik olarak silmek için Index Lifecycle Management (ILM) kullanabilirsiniz:

1. **Stack Management** → **Index Lifecycle Policies**
2. **Create policy** butonuna tıklayın
3. Policy adı: `microservices-logs-policy`
4. Phases:
   - **Hot**: 7 gün
   - **Delete**: 30 gün sonra sil
5. **Save policy**

---

## 🎨 Örnek Dashboard JSON

Hazır bir dashboard import etmek için:

1. **Stack Management** → **Saved Objects**
2. **Import** butonuna tıklayın
3. Aşağıdaki JSON'ı kullanın veya kendi dashboard'unuzu export edin

---

## 📚 Faydalı Linkler

- **Elasticsearch Docs:** https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
- **Kibana Docs:** https://www.elastic.co/guide/en/kibana/current/index.html
- **KQL Syntax:** https://www.elastic.co/guide/en/kibana/current/kuery-query.html
- **Logstash Docs:** https://www.elastic.co/guide/en/logstash/current/index.html

---

## 🚦 Hızlı Başlangıç Checklist

- [ ] ELK stack'i başlat: `docker-compose up -d`
- [ ] Elasticsearch'ün hazır olmasını bekle (2-3 dakika)
- [ ] Kibana'ya eriş: http://localhost:5601
- [ ] Index pattern oluştur: `microservices-logs-*`
- [ ] Discover'a git ve logları görüntüle
- [ ] İlk filtreyi oluştur: `log_level: "ERROR"`
- [ ] Dashboard oluştur ve visualizationları ekle

---

## 💡 İpuçları

1. **Zaman Aralığı:** Kibana'da sağ üst köşeden zaman aralığını ayarlayın
2. **Auto-refresh:** Canlı log izlemek için auto-refresh'i aktif edin (örn: 10 saniye)
3. **Field Filters:** Sol taraftaki field listesinden hızlı filtreleme yapın
4. **Saved Searches:** Sık kullandığınız aramaları kaydedin
5. **Dark Mode:** Kibana'da dark mode için Stack Management → Advanced Settings → Theme

---

## 🎯 Sonraki Adımlar

1. ✅ ELK stack'i başarıyla kurdunuz
2. 📊 Kendi dashboard'larınızı oluşturun
3. 🔔 Kritik hatalar için alertler kurun
4. 📈 Performans metriklerini izleyin
5. 🔍 Log retention policy'si belirleyin

**Başarılar! 🎉**
