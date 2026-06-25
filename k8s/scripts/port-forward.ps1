# ============================================================
# port-forward.ps1
# Opens port-forwards to access services from localhost
# Run from project root: .\k8s\scripts\port-forward.ps1
# Press Ctrl+C to stop all port-forwards
# ============================================================

Write-Host "=== Starting port-forwards ===" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop all."
Write-Host ""

$forwards = @(
    # Apps
    @{ ns="apps";         svc="api-gateway";         local=8080; remote=8080 },
    # Infra (for local DB tools like DBeaver)
    @{ ns="infra";        svc="postgres-postgresql";  local=5432; remote=5432 },
    @{ ns="infra";        svc="redis-master";         local=6379; remote=6379 },
    @{ ns="infra";        svc="mongodb";              local=27017; remote=27017 },
    @{ ns="infra";        svc="kafka";                local=9092; remote=9092 },
    @{ ns="infra";        svc="rabbitmq";             local=15672; remote=15672 },
    # Observability
    @{ ns="observability"; svc="prometheus-stack-kube-prom-prometheus"; local=9090; remote=9090 },
    @{ ns="observability"; svc="prometheus-stack-grafana"; local=3000; remote=3000 },
    @{ ns="observability"; svc="jaeger";              local=16686; remote=16686 },
    # Logging
    @{ ns="logging";      svc="kibana-kibana";        local=5601; remote=5601 },
    @{ ns="logging";      svc="elasticsearch-master"; local=9200; remote=9200 },
    # Tools
    @{ ns="tools";        svc="akhq";                 local=9000; remote=9000 },
    @{ ns="tools";        svc="maildev";              local=1080; remote=1080 }
)

$jobs = @()
foreach ($f in $forwards) {
    Write-Host "  $($f.ns)/$($f.svc): localhost:$($f.local) -> $($f.remote)" -ForegroundColor Gray
    $job = Start-Job -ScriptBlock {
        param($ns, $svc, $local, $remote)
        kubectl port-forward "svc/$svc" "${local}:${remote}" -n $ns 2>&1
    } -ArgumentList $f.ns, $f.svc, $f.local, $f.remote
    $jobs += $job
}

Write-Host ""
Write-Host "Port-forwards active:" -ForegroundColor Green
Write-Host "  API Gateway:   http://localhost:8080"
Write-Host "  Grafana:       http://localhost:3000  (admin/admin)"
Write-Host "  Jaeger:        http://localhost:16686"
Write-Host "  Kibana:        http://localhost:5601"
Write-Host "  AKHQ:          http://localhost:9000"
Write-Host "  MailDev:       http://localhost:1080"
Write-Host "  Elasticsearch: http://localhost:9200"
Write-Host "  PostgreSQL:    localhost:5432"
Write-Host "  Redis:         localhost:6379"
Write-Host "  Kafka:         localhost:9092"
Write-Host ""
Write-Host "Press Enter to stop all port-forwards..." -ForegroundColor Yellow
$null = Read-Host

$jobs | Stop-Job
$jobs | Remove-Job
Write-Host "Port-forwards stopped." -ForegroundColor Green
