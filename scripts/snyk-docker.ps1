# Run Snyk scan via Docker
# Requirements: Docker must be running and SNYK_TOKEN environment variable must be set.

if (-not $env:SNYK_TOKEN) {
    Write-Host "Error: SNYK_TOKEN environment variable is not set." -ForegroundColor Red
    Write-Host "Please set it using: `$env:SNYK_TOKEN = 'your_token_here'"
    exit 1
}

Write-Host "Starting Snyk scan for Monat E-Commerce..." -ForegroundColor Cyan

docker run --rm `
  -v "${PWD}:/app" `
  -w /app `
  -e SNYK_TOKEN=$env:SNYK_TOKEN `
  snyk/snyk-cli:maven `
  snyk test --all-projects
