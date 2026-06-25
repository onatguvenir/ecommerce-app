# ============================================================
# 02-deploy-logging.ps1
# Deploys ELK stack (Elasticsearch, Logstash, Kibana, Filebeat)
# into the 'logging' namespace
# Run from project root: .\k8s\scripts\02-deploy-logging.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "=== Adding Elastic Helm repo ===" -ForegroundColor Cyan
helm repo add elastic https://helm.elastic.co
helm repo update

Write-Host ""
Write-Host "=== Deploying Elasticsearch ===" -ForegroundColor Cyan
$esExists = helm status elasticsearch -n logging 2>$null
if ($esExists) {
    helm upgrade elasticsearch elastic/elasticsearch -n logging -f "$ProjectRoot\k8s\logging\elasticsearch\helm-values.yaml"
} else {
    helm install elasticsearch elastic/elasticsearch -n logging -f "$ProjectRoot\k8s\logging\elasticsearch\helm-values.yaml"
}

Write-Host "Waiting for Elasticsearch to be ready (this takes ~2 min)..." -ForegroundColor Yellow
kubectl wait --for=condition=Ready pods -l app=elasticsearch-master -n logging --timeout=300s

Write-Host ""
Write-Host "=== Deploying Logstash ===" -ForegroundColor Cyan
kubectl apply -f "$ProjectRoot\k8s\logging\logstash\configmap.yaml"
kubectl apply -f "$ProjectRoot\k8s\logging\logstash\deployment.yaml"

Write-Host ""
Write-Host "=== Deploying Kibana ===" -ForegroundColor Cyan
$kbExists = helm status kibana -n logging 2>$null
if ($kbExists) {
    helm upgrade kibana elastic/kibana -n logging -f "$ProjectRoot\k8s\logging\kibana\helm-values.yaml"
} else {
    helm install kibana elastic/kibana -n logging -f "$ProjectRoot\k8s\logging\kibana\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying Filebeat DaemonSet ===" -ForegroundColor Cyan
kubectl apply -f "$ProjectRoot\k8s\logging\filebeat\daemonset.yaml"

Write-Host ""
Write-Host "=== Logging stack deployed ===" -ForegroundColor Green
kubectl get pods -n logging
Write-Host "Kibana: http://localhost:5601 (after port-forward or NodePort 30561)"
Write-Host "Next step: .\k8s\scripts\03-deploy-observability.ps1"
