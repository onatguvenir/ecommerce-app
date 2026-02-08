# Base Docker Configuration

This directory contains shared Docker resources for the e-commerce platform.

## Directory Structure

```
docker/
├── init-databases.sql          # PostgreSQL initialization script
├── postgres-init.sh            # PostgreSQL multi-database init
├── prometheus/
│   └── prometheus.yml          # Prometheus monitoring config
└── README.md                   # This file
```

## PostgreSQL Multi-Database Initialization

The `postgres-init.sh` script automatically creates all required databases when PostgreSQL starts:
- `userdb` - User Service
- `orderdb` - Order Service
- `inventorydb` - Inventory Service
- `paymentdb` - Payment Service

### Usage

Mounted in docker-compose.yml:
```yaml
volumes:
  - ./docker/postgres-init.sh:/docker-entrypoint-initdb.d/init.sh
```

## Prometheus Configuration

The `prometheus.yml` file defines scraping targets for all microservices:
- Service discovery via static configs
- Metrics endpoints on `/actuator/prometheus`
- 15-second scrape interval

### Adding New Services

Add to `prometheus.yml`:
```yaml
- job_name: 'new-service'
  static_configs:
    - targets: ['new-service:8080']
```

## Environment Variables

Common environment variables across services:

### Database Configuration
- `SPRING_DATASOURCE_URL` - PostgreSQL JDBC URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### MongoDB Configuration
- `SPRING_DATA_MONGODB_URI` - MongoDB connection string

### Redis Configuration
- `SPRING_DATA_REDIS_HOST` - Redis host
- `SPRING_DATA_REDIS_PORT` - Redis port

### Kafka Configuration
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers

### Elasticsearch Configuration
- `SPRING_ELASTICSEARCH_URIS` - Elasticsearch URL

## Health Checks

All services include health checks:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

## Networks

Single bridge network for all services:
```yaml
networks:
  monat-network:
    driver: bridge
```

## Volumes

Persistent storage:
- `postgres_data` - PostgreSQL data
- `mongodb_data` - MongoDB data
- `redis_data` - Redis data
- `elasticsearch_data` - Elasticsearch indices
- `prometheus_data` - Prometheus metrics
- `grafana_data` - Grafana dashboards

## Best Practices

1. **Use Environment Variables**: Never hardcode credentials
2. **Health Checks**: Always define health checks for services
3. **Resource Limits**: Set memory and CPU limits in production
4. **Named Volumes**: Use named volumes for data persistence
5. **Logging**: Configure log drivers for centralized logging

## Production Considerations

For production deployments:
1. Use Kubernetes Secrets instead of environment variables
2. Implement proper log aggregation (ELK, Loki)
3. Use external configuration management (Consul, etcd)
4. Enable TLS/SSL for all inter-service communication
5. Implement proper backup strategies for volumes
