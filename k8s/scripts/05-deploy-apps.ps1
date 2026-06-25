# ============================================================
# 05-deploy-apps.ps1
# Deploys all 9 microservices into the 'apps' namespace
# Run from project root: .\k8s\scripts\05-deploy-apps.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$AppsDir = "$ProjectRoot\k8s\apps"

Write-Host "=== Deploying shared ConfigMap ===" -ForegroundColor Cyan
kubectl apply -f "$AppsDir\_env-configmap.yaml"

$Services = @(
    "api-gateway",
    "user-service",
    "product-service",
    "inventory-service",
    "cart-service",
    "payment-service",    # payment before order (order depends on payment gRPC)
    "order-service",
    "notification-service",
    "fraud-service"
)

foreach ($svc in $Services) {
    Write-Host ""
    Write-Host "--- Deploying $svc ---" -ForegroundColor Yellow
    kubectl apply -f "$AppsDir\$svc\deployment.yaml"
    kubectl apply -f "$AppsDir\$svc\service.yaml"

    # Apply ingress only for api-gateway
    $ingressPath = "$AppsDir\$svc\ingress.yaml"
    if (Test-Path $ingressPath) {
        kubectl apply -f $ingressPath
    }
}

Write-Host ""
Write-Host "=== Waiting for all app pods to be ready (up to 10 min) ===" -ForegroundColor Cyan
Write-Host "Spring Boot cold starts can take 90-180s per service..." -ForegroundColor Yellow
kubectl wait --for=condition=Ready pods --all -n apps --timeout=600s

Write-Host ""
Write-Host "=== Microservices deployed ===" -ForegroundColor Green
kubectl get pods -n apps
Write-Host ""
Write-Host "API Gateway: http://monat.local (via Traefik Ingress)"
Write-Host "Or via port-forward: .\k8s\scripts\port-forward.ps1"
Write-Host "Next step: .\k8s\scripts\06-deploy-tools.ps1"
