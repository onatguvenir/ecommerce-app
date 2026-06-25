# ============================================================
# 00-setup-cluster.ps1
# Creates k3d cluster with local registry and applies namespaces
# Run from project root: .\k8s\scripts\00-setup-cluster.ps1
# ============================================================

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "=== Creating k3d cluster 'monat' ===" -ForegroundColor Cyan

# Check if cluster already exists
$existing = k3d cluster list --no-headers 2>$null | Select-String "monat"
if ($existing) {
    Write-Host "Cluster 'monat' already exists. Skipping creation." -ForegroundColor Yellow
} else {
    k3d cluster create --config "$ProjectRoot\k8s\cluster\k3d-config.yaml"
    Write-Host "Cluster created." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Merging kubeconfig ===" -ForegroundColor Cyan
k3d kubeconfig merge monat --kubeconfig-switch-context
kubectl config use-context k3d-monat

Write-Host ""
Write-Host "=== Applying namespaces ===" -ForegroundColor Cyan
kubectl apply -f "$ProjectRoot\k8s\namespaces.yaml"

Write-Host ""
Write-Host "=== Waiting for cluster to be ready ===" -ForegroundColor Cyan
kubectl wait --for=condition=Ready nodes --all --timeout=60s

Write-Host ""
Write-Host "=== Adding monat.local to hosts file ===" -ForegroundColor Cyan
$hostsFile = "C:\Windows\System32\drivers\etc\hosts"
$hostsEntry = "127.0.0.1 monat.local"
$content = Get-Content $hostsFile -Raw
if ($content -notmatch "monat.local") {
    Write-Host "Adding '$hostsEntry' to $hostsFile" -ForegroundColor Yellow
    Write-Host "NOTE: Run PowerShell as Administrator if this step fails." -ForegroundColor Yellow
    Add-Content -Path $hostsFile -Value "`n$hostsEntry"
    Write-Host "Hosts entry added." -ForegroundColor Green
} else {
    Write-Host "monat.local already in hosts file." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Cluster setup complete ===" -ForegroundColor Green
Write-Host "Registry: localhost:5001 (push) / k3d-monat-registry:5000 (pull within cluster)"
Write-Host "Next step: .\k8s\scripts\01-deploy-infra.ps1"
