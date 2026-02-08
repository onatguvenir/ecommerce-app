# Kubernetes Deployment Guide

## Prerequisites
- Kubernetes cluster (1.28+)
- kubectl configured
- Docker images built and pushed to registry

## Quick Deploy

### 1. Create Namespace
```bash
kubectl create namespace monat-ecommerce
kubectl config set-context --current --namespace=monat-ecommerce
```

### 2. Deploy Infrastructure (Database StatefulSets, Redis, Kafka)
```bash
# PostgreSQL
kubectl apply -f k8s/infrastructure/postgres-statefulset.yaml

# MongoDB
kubectl apply -f k8s/infrastructure/mongodb-statefulset.yaml

# Redis
kubectl apply -f k8s/infrastructure/redis-deployment.yaml

# Elasticsearch
kubectl apply -f k8s/infrastructure/elasticsearch-statefulset.yaml

# Kafka (requires Zookeeper)
kubectl apply -f k8s/infrastructure/zookeeper-statefulset.yaml
kubectl apply -f k8s/infrastructure/kafka-statefulset.yaml
```

### 3. Create Secrets and ConfigMaps
```bash
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
```

### 4. Deploy Microservices
```bash
# Deploy services in dependency order
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/inventory-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/cart-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/notification-service-deployment.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml
```

### 5. Verify Deployment
```bash
# Check all pods
kubectl get pods

# Check services
kubectl get svc

# Watch pod status
kubectl get pods -w
```

## Access Services

### Using LoadBalancer (Cloud)
```bash
# Get external IP
kubectl get svc api-gateway-service

# Access via external IP
curl http://<EXTERNAL-IP>:8080/api/products
```

### Using Port Forward (Local)
```bash
# Forward API Gateway
kubectl port-forward svc/api-gateway-service 8080:8080

# Access locally
curl http://localhost:8080/api/products
```

### Using Ingress (Recommended)
```bash
# Apply ingress
kubectl apply -f k8s/ingress.yaml

# Access via domain
curl http://monat-ecommerce.local/api/products
```

## Scaling

### Manual Scaling
```bash
# Scale Order Service
kubectl scale deployment order-service --replicas=5

# Scale Inventory Service
kubectl scale deployment inventory-service --replicas=4
```

### Auto-Scaling (HPA)
Already configured for Order Service in deployment manifest.

```bash
# Check HPA status
kubectl get hpa

# Watch autoscaling
kubectl get hpa -w
```

## Monitoring

### Pod Logs
```bash
# View logs
kubectl logs -f deployment/order-service

# View logs from specific pod
kubectl logs -f pod/order-service-xxxxx-xxxxx

# Tail last 100 lines
kubectl logs --tail=100 deployment/order-service
```

### Pod Status
```bash
# Describe pod
kubectl describe pod order-service-xxxxx-xxxxx

# Check resource usage
kubectl top pods
kubectl top nodes
```

### Events
```bash
# Watch events
kubectl get events --watch

# Sort by timestamp
kubectl get events --sort-by='.metadata.creationTimestamp'
```

## Troubleshooting

### Pod Not Starting
```bash
# Check pod status
kubectl describe pod <pod-name>

# Check logs
kubectl logs <pod-name>

# Check previous container logs (if crashed)
kubectl logs <pod-name> --previous
```

### Service Not Reachable
```bash
# Check service endpoints
kubectl get endpoints

# Test service from another pod
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
wget -O- http://order-service:8085/actuator/health
```

### Database Connection Issues
```bash
# Check PostgreSQL pod
kubectl logs -f statefulset/postgres

# Connect to database
kubectl exec -it postgres-0 -- psql -U postgres

# List databases
\l
```

## Resource Management

### Resource Requests and Limits
Already configured in deployment manifests:
- API Gateway: 512Mi-1Gi RAM, 250m-1000m CPU
- Order Service: 768Mi-1.5Gi RAM, 500m-1.5 CPU
- Others: 512Mi-1Gi RAM, 250m-1 CPU

### Pod Disruption Budget
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: order-service-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: order-service
```

## Rolling Updates

```bash
# Update image
kubectl set image deployment/order-service order-service=monat/order-service:v2

# Check rollout status
kubectl rollout status deployment/order-service

# Rollback if needed
kubectl rollout undo deployment/order-service
```

## Cleanup

```bash
# Delete all services
kubectl delete -f k8s/

# Delete namespace
kubectl delete namespace monat-ecommerce
```

## Production Considerations

### Security
- [ ] Use proper secrets management (Vault, AWS Secrets Manager)
- [ ] Enable RBAC
- [ ] Network policies for pod-to-pod communication
- [ ] Pod security policies
- [ ] TLS for all services

### High Availability
- [ ] Multi-zone deployment
- [ ] Database replication
- [ ] Redis Sentinel or Cluster mode
- [ ] Kafka cluster (3+ brokers)

### Monitoring & Observability
- [ ] Deploy Prometheus Operator
- [ ] Set up Grafana dashboards
- [ ] Configure alerts
- [ ] Deploy Jaeger for distributed tracing
- [ ] Set up centralized logging (ELK/Loki)

### Service Mesh (Istio)
- [ ] Install Istio
- [ ] Enable sidecar injection
- [ ] Configure VirtualServices
- [ ] Set up circuit breakers
- [ ] Enable mTLS

## Helm Chart (Coming Soon)

For easier deployment, use Helm:
```bash
helm install monat-ecommerce ./helm/monat-ecommerce
```
