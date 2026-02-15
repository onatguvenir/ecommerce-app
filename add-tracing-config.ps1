# Distributed Tracing Configuration Script

# Bu script tüm servislerin application.yml dosyalarına tracing konfigürasyonu ekler

$services = @(
    "product-service",
    "order-service",
    "cart-service",
    "inventory-service",
    "payment-service",
    "notification-service"
)

$tracingConfig = @"

# Distributed Tracing
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of requests (reduce in production to 0.1)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
"@

foreach ($service in $services) {
    $ymlPath = "$service\src\main\resources\application.yml"
    
    if (Test-Path $ymlPath) {
        Write-Host "Updating $service..." -ForegroundColor Green
        
        # Read current content
        $content = Get-Content $ymlPath -Raw
        
        # Update logging pattern if exists
        if ($content -match "logging:") {
            $content = $content -replace "console: `"%d\{yyyy-MM-dd HH:mm:ss\} - %msg%n`"", "console: `"%d{yyyy-MM-dd HH:mm:ss} [%X{traceId:-},%X{spanId:-}] - %msg%n`""
            $content = $content -replace "file: `"%d\{yyyy-MM-dd HH:mm:ss\} \[%thread\] %-5level %logger\{36\} - %msg%n`"", "file: `"%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n`""
        }
        
        # Add tracing config if not exists
        if ($content -notmatch "management.tracing") {
            $content += $tracingConfig
        }
        
        # Write back
        Set-Content -Path $ymlPath -Value $content -NoNewline
        
        Write-Host "✓ $service updated" -ForegroundColor Cyan
    } else {
        Write-Host "✗ $ymlPath not found" -ForegroundColor Red
    }
}

Write-Host "`nDone! All services configured for distributed tracing." -ForegroundColor Green
Write-Host "Don't forget to rebuild: mvn clean install -DskipTests" -ForegroundColor Yellow
