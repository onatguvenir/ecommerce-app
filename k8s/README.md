# Monat E-Commerce — Local Kubernetes Setup

## Ön Koşullar

```powershell
winget install k3d               # https://k3d.io
winget install Kubernetes.kubectl
winget install Helm.Helm
```

Docker Desktop → Resources → Memory: **en az 16 GB** (20 GB önerilir)

---

## Hızlı Başlangıç (sırayla çalıştır)

```powershell
# Proje kökünden çalıştır
.\k8s\scripts\00-setup-cluster.ps1     # Cluster + registry + namespaces
.\k8s\scripts\01-deploy-infra.ps1      # PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ
.\k8s\scripts\02-deploy-logging.ps1    # Elasticsearch, Logstash, Kibana, Filebeat
.\k8s\scripts\03-deploy-observability.ps1  # Prometheus, Grafana, Jaeger, OTel
.\k8s\scripts\04-build-push-images.ps1 # Docker build + push (uzun sürer ~15 dk)
.\k8s\scripts\05-deploy-apps.ps1       # 9 mikroservis
.\k8s\scripts\06-deploy-tools.ps1      # AKHQ, MailDev, Debezium, RedisInsight
.\k8s\scripts\port-forward.ps1         # Port-forward açar
```

---

## Servis Adresleri (port-forward sonrası)

| Servis | URL | Kimlik |
|--------|-----|--------|
| API Gateway | http://monat.local veya http://localhost:8080 | — |
| Grafana | http://localhost:3000 | admin/admin |
| Jaeger | http://localhost:16686 | — |
| Kibana | http://localhost:5601 | — |
| AKHQ | http://localhost:9000 | — |
| MailDev | http://localhost:1080 | — |
| Elasticsearch | http://localhost:9200 | — |
| PostgreSQL | localhost:5432 | postgres/postgres |
| Redis | localhost:6379 | — |
| Kafka | localhost:9092 | — |

---

## Namespace Yapısı

| Namespace | İçerik |
|-----------|--------|
| `infra` | PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ |
| `logging` | Elasticsearch, Logstash, Kibana, Filebeat |
| `observability` | Prometheus, Grafana, Jaeger, OTel Collector |
| `apps` | 9 mikroservis |
| `tools` | AKHQ, Debezium, RedisInsight, MailDev, SonarQube |

---

## Tek Servis Yeniden Deploy

```powershell
# Image yeniden build et ve push
.\k8s\scripts\04-build-push-images.ps1 order-service

# Deployment'ı restart et (yeni image'ı çeker)
kubectl rollout restart deployment/order-service -n apps

# Pod durumunu izle
kubectl rollout status deployment/order-service -n apps
```

---

## Hata Ayıklama

```powershell
# Tüm pod'ların durumu
kubectl get pods -A

# Belirli pod'un logları
kubectl logs -f deployment/order-service -n apps

# Init container loglarına bak
kubectl logs pod/<pod-name> -c wait-for-kafka -n apps

# Pod içine gir
kubectl exec -it deployment/order-service -n apps -- sh

# Tüm servis endpoint'leri
kubectl get svc -A
```

---

## Cluster Sil

```powershell
k3d cluster delete monat
```

---

## Önemli Notlar

1. **MongoDB**: Single-member replica set (rs0) — product-service için zorunlu
2. **PostgreSQL**: `wal_level=logical` — Debezium CDC için zorunlu
3. **Kafka**: KRaft modu, aynı Cluster ID docker-compose ile aynı
4. **Image format**: `k3d-monat-registry:5000/monat/<service>:latest`
5. **Env var override**: Servisler `--spring.profiles.active=docker` ile başlar; K8s env var'ları `application-docker.yml`'i override eder
6. **`monat.local`**: `C:\Windows\System32\drivers\etc\hosts`'a `127.0.0.1 monat.local` eklenmesi gerekir (00-setup-cluster.ps1 otomatik yapar, admin yetki ister)
