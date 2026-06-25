# ============================================================
# 04-build-push-images.ps1
# Builds Docker images for all 9 microservices and pushes them
# to the k3d local registry (localhost:5001)
# Run from project root: .\k8s\scripts\04-build-push-images.ps1
# Optional: pass service names to build only specific ones
#   .\k8s\scripts\04-build-push-images.ps1 order-service payment-service
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Registry = "localhost:5001/monat"

$AllServices = @(
    "api-gateway",
    "user-service",
    "product-service",
    "inventory-service",
    "cart-service",
    "order-service",
    "payment-service",
    "notification-service",
    "fraud-service"
)

# If args provided, build only those services
$Services = if ($args.Count -gt 0) { $args } else { $AllServices }

Write-Host "=== Building and pushing images ===" -ForegroundColor Cyan
Write-Host "Registry: $Registry"
Write-Host "Services: $($Services -join ', ')"
Write-Host ""

foreach ($svc in $Services) {
    $dockerfilePath = "$ProjectRoot\$svc\Dockerfile"
    if (-not (Test-Path $dockerfilePath)) {
        Write-Host "WARNING: Dockerfile not found for $svc — skipping" -ForegroundColor Yellow
        continue
    }

    $tag = "$Registry/${svc}:latest"
    Write-Host "--- Building $svc ---" -ForegroundColor Yellow
    docker build -t $tag -f $dockerfilePath $ProjectRoot
    if ($LASTEXITCODE -ne 0) { throw "Build failed for $svc" }

    Write-Host "--- Pushing $svc ---" -ForegroundColor Yellow
    docker push $tag
    if ($LASTEXITCODE -ne 0) { throw "Push failed for $svc" }

    Write-Host "$svc done." -ForegroundColor Green
    Write-Host ""
}

Write-Host "=== All images built and pushed ===" -ForegroundColor Green
Write-Host "Next step: .\k8s\scripts\05-deploy-apps.ps1"
