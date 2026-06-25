# ============================================================
# 03-deploy-observability.ps1
# Deploys Prometheus+Grafana, Jaeger, OTel Collector
# into the 'observability' namespace
# Run from project root: .\k8s\scripts\03-deploy-observability.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "=== Adding Prometheus Community Helm repo ===" -ForegroundColor Cyan
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

Write-Host ""
Write-Host "=== Deploying kube-prometheus-stack (Prometheus + Grafana) ===" -ForegroundColor Cyan
$promExists = helm status prometheus-stack -n observability 2>$null
if ($promExists) {
    helm upgrade prometheus-stack prometheus-community/kube-prometheus-stack -n observability `
        -f "$ProjectRoot\k8s\observability\prometheus-stack\helm-values.yaml"
} else {
    helm install prometheus-stack prometheus-community/kube-prometheus-stack -n observability `
        -f "$ProjectRoot\k8s\observability\prometheus-stack\helm-values.yaml"
}

Write-Host ""
Write-Host "=== Deploying Jaeger all-in-one ===" -ForegroundColor Cyan
kubectl apply -f "$ProjectRoot\k8s\observability\jaeger\all-in-one.yaml"

Write-Host ""
Write-Host "=== Deploying OTel Collector ===" -ForegroundColor Cyan
kubectl apply -f "$ProjectRoot\k8s\observability\otel-collector\configmap.yaml"
kubectl apply -f "$ProjectRoot\k8s\observability\otel-collector\deployment.yaml"

Write-Host ""
Write-Host "=== Waiting for observability pods ===" -ForegroundColor Cyan
kubectl wait --for=condition=Ready pods --all -n observability --timeout=180s

Write-Host ""
Write-Host "=== Observability stack deployed ===" -ForegroundColor Green
kubectl get pods -n observability
Write-Host "Grafana:  http://localhost:3000 (admin/admin)"
Write-Host "Jaeger:   http://localhost:16686"
Write-Host "Next step: .\k8s\scripts\04-build-push-images.ps1"
