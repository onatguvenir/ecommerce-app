# Merkezi Log Görüntüleme Kılavuzu

## Yöntem 1: Docker Compose Logs (Hızlı ve Kolay)

### Tüm Servislerin Loglarını Görüntüleme

```bash
# Tüm servislerin loglarını canlı izleme
docker-compose logs -f

# Son 100 satırı göster ve canlı izle
docker-compose logs --tail=100 -f

# Sadece hataları göster
docker-compose logs -f | grep -i error

# Belirli bir zaman aralığı
docker-compose logs --since 30m
docker-compose logs --since 2024-01-01T00:00:00
```

### Belirli Servislerin Logları

```bash
# Tek bir servis
docker-compose logs -f user-service

# Birden fazla servis
docker-compose logs -f user-service product-service order-service

# Mikroservisler (veritabanları hariç)
docker-compose logs -f user-service product-service inventory-service cart-service order-service payment-service notification-service
```

### Log Filtreleme

```bash
# Sadece ERROR seviyesi
docker-compose logs -f | grep ERROR

# Belirli bir kelime ara
docker-compose logs -f | grep "Order created"

# Birden fazla pattern
docker-compose logs -f | grep -E "ERROR|WARN"

# Timestamp ile
docker-compose logs -f --timestamps
```

## Yöntem 2: ELK Stack (Önerilen - Production Ready)

### Kurulum

ELK stack zaten `docker-compose.yml` dosyasına eklenmiştir. Başlatmak için:

```bash
# Tüm servisleri başlat (ELK dahil)
docker-compose up -d

# Sadece ELK stack'i başlat
docker-compose up -d elasticsearch logstash kibana filebeat
```

### Kibana'ya Erişim
- URL: http://localhost:5601
- Index pattern: `microservices-logs-*`

### Avantajları:
- ✅ Güçlü arama ve filtreleme (KQL, Lucene)
- ✅ Zengin görselleştirme ve dashboard'lar
- ✅ Alert ve notification desteği
- ✅ Index lifecycle management
- ✅ Endüstri standardı

**Detaylı kullanım için `ELK-SETUP-GUIDE.md` dosyasına bakın.**

## Yöntem 4: Dozzle (Basit Web UI)

En basit web tabanlı log görüntüleyici:

```yaml
  dozzle:
    image: amir20/dozzle:latest
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "8888:8080"
    networks:
      - ecommerce-network
```

Erişim: http://localhost:8888

### Avantajları:
- ✅ Kurulum gerektirmez
- ✅ Hafif ve hızlı
- ✅ Gerçek zamanlı log akışı
- ✅ Multi-container desteği
- ✅ Arama ve filtreleme

## Yöntem 5: Windows PowerShell ile Gelişmiş Filtreleme

```powershell
# Tüm servislerin loglarını renkli göster
docker-compose logs -f | Select-String -Pattern "ERROR" -Context 2,2

# JSON logları parse et
docker-compose logs -f user-service | ConvertFrom-Json | Format-Table

# Belirli bir zaman aralığı
docker-compose logs --since (Get-Date).AddHours(-1)
```

## Önerilen Yaklaşım

### Development için:
```bash
# Terminal 1: Tüm servisler
docker-compose logs -f --tail=50

# Terminal 2: Sadece hatalar
docker-compose logs -f | grep -E "ERROR|WARN|Exception"
```

### Production için:
1. **ELK Stack** (Güçlü arama, dashboard, alert)
2. **Dozzle** (Basit, kurulum gerektirmez)

**Detaylı ELK kullanımı için:** `ELK-SETUP-GUIDE.md`

## Hızlı Başlangıç: Dozzle ile

En hızlı çözüm için `docker-compose.yml`'e ekleyin:

```yaml
  dozzle:
    container_name: dozzle
    image: amir20/dozzle:latest
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    ports:
      - "8888:8080"
    networks:
      - ecommerce-network
```

Sonra:
```bash
docker-compose up -d dozzle
```

Tarayıcıda açın: http://localhost:8888

## Log Seviyeleri

Servislerinizde log seviyelerini ayarlamak için `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.monat.ecommerce: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

## Faydalı Komutlar

```bash
# Container'ların durumunu kontrol et
docker-compose ps

# Belirli bir container'ın loglarını dosyaya kaydet
docker-compose logs user-service > user-service-logs.txt

# Log boyutlarını kontrol et
docker system df

# Eski logları temizle
docker system prune -a --volumes
```

## Sorun Giderme

### Loglar görünmüyor?
```bash
# Container'ın çalıştığından emin olun
docker-compose ps

# Container'ı yeniden başlatın
docker-compose restart user-service
```

### Çok fazla log var?
```bash
# Log seviyesini INFO veya WARN'a çekin
# application.yml'de logging.level.root: WARN
```

### Disk doldu?
```bash
# Eski logları temizle
docker system prune -a --volumes -f
```
