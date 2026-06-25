# ============================================================
# 06-deploy-tools.ps1
# Deploys admin tools: AKHQ, Debezium Connect, RedisInsight,
# MailDev, SonarQube into the 'tools' namespace
# Run from project root: .\k8s\scripts\06-deploy-tools.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ToolsDir = "$ProjectRoot\k8s\tools"

Write-Host "=== Deploying AKHQ (Kafka UI) ===" -ForegroundColor Cyan
kubectl apply -f "$ToolsDir\akhq\deployment.yaml"

Write-Host ""
Write-Host "=== Deploying RedisInsight ===" -ForegroundColor Cyan
kubectl apply -f "$ToolsDir\redisinsight\deployment.yaml"

Write-Host ""
Write-Host "=== Deploying MailDev ===" -ForegroundColor Cyan
kubectl apply -f "$ToolsDir\maildev\deployment.yaml"

Write-Host ""
Write-Host "=== Deploying Debezium Connect ===" -ForegroundColor Cyan
kubectl apply -f "$ToolsDir\debezium-connect\deployment.yaml"

Write-Host "Waiting for Debezium Connect to be ready (~2 min)..." -ForegroundColor Yellow
kubectl wait --for=condition=Ready pods -l app=debezium-connect -n tools --timeout=180s

Write-Host ""
Write-Host "=== Registering Debezium connectors ===" -ForegroundColor Cyan
kubectl apply -f "$ToolsDir\debezium-connect\connector-init-job.yaml"

Write-Host ""
Write-Host "=== Deploying SonarQube (optional, heavy ~2GB) ===" -ForegroundColor Yellow
$response = Read-Host "Deploy SonarQube? (y/N)"
if ($response -match "^[Yy]") {
    $sonarExists = helm status sonarqube -n tools 2>$null
    if ($sonarExists) {
        helm upgrade sonarqube bitnami/sonarqube -n tools -f "$ProjectRoot\k8s\tools\sonarqube\helm-values.yaml"
    } else {
        helm install sonarqube bitnami/sonarqube -n tools -f "$ProjectRoot\k8s\tools\sonarqube\helm-values.yaml"
    }
} else {
    Write-Host "SonarQube skipped." -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Tools deployed ===" -ForegroundColor Green
kubectl get pods -n tools
Write-Host ""
Write-Host "AKHQ:         http://localhost:9000  (NodePort 30900)"
Write-Host "RedisInsight: http://localhost:5540  (NodePort 30554)"
Write-Host "MailDev:      use port-forward (see port-forward.ps1)"
Write-Host "Debezium:     kubectl logs job/connector-init -n tools"
