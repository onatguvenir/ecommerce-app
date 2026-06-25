# ============================================================
# 01-deploy-infra.ps1
# Deploys infrastructure services via Helm (PostgreSQL, MongoDB,
# Redis, Kafka, RabbitMQ) into the 'infra' namespace
# Run from project root: .\k8s\scripts\01-deploy-infra.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "=== Adding Helm repos ===" -ForegroundColor Cyan
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

Write-Host ""
Write-Host "=== Deploying PostgreSQL ===" -ForegroundColor Cyan
# ConfigMap with init SQL must exist before PostgreSQL starts
kubectl apply -f "$ProjectRoot\k8s\infra\postgres\init-configmap.yaml"

$pgExists = helm status postgres -n infra 2>$null
if ($pgExists) {
    Write-Host "PostgreSQL already deployed, upgrading..." -ForegroundColor Yellow
    helm upgrade postgres bitnami/postgresql -n infra -f "$ProjectRoot\k8s\infra\postgres\helm-values.yaml"
} else {
    helm install postgres bitnami/postgresql -n infra -f "$ProjectRoot\k8s\infra\postgres\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying MongoDB ===" -ForegroundColor Cyan
$mongoExists = helm status mongodb -n infra 2>$null
if ($mongoExists) {
    helm upgrade mongodb bitnami/mongodb -n infra -f "$ProjectRoot\k8s\infra\mongodb\helm-values.yaml"
} else {
    helm install mongodb bitnami/mongodb -n infra -f "$ProjectRoot\k8s\infra\mongodb\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying Redis ===" -ForegroundColor Cyan
$redisExists = helm status redis -n infra 2>$null
if ($redisExists) {
    helm upgrade redis bitnami/redis -n infra -f "$ProjectRoot\k8s\infra\redis\helm-values.yaml"
} else {
    helm install redis bitnami/redis -n infra -f "$ProjectRoot\k8s\infra\redis\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying Kafka (KRaft) ===" -ForegroundColor Cyan
$kafkaExists = helm status kafka -n infra 2>$null
if ($kafkaExists) {
    helm upgrade kafka bitnami/kafka -n infra -f "$ProjectRoot\k8s\infra\kafka\helm-values.yaml"
} else {
    helm install kafka bitnami/kafka -n infra -f "$ProjectRoot\k8s\infra\kafka\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying RabbitMQ ===" -ForegroundColor Cyan
$rmqExists = helm status rabbitmq -n infra 2>$null
if ($rmqExists) {
    helm upgrade rabbitmq bitnami/rabbitmq -n infra -f "$ProjectRoot\k8s\infra\rabbitmq\helm-values.yaml"
} else {
    helm install rabbitmq bitnami/rabbitmq -n infra -f "$ProjectRoot\k8s\infra\rabbitmq\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Waiting for infra pods to be ready ===" -ForegroundColor Cyan
kubectl wait --for=condition=Ready pods --all -n infra --timeout=300s

Write-Host ""
Write-Host "=== Infrastructure deployed ===" -ForegroundColor Green
kubectl get pods -n infra
Write-Host "Next step: .\k8s\scripts\02-deploy-logging.ps1"
