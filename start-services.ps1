#!/usr/bin/env pwsh
# Version simplificada - solo inicia los servicios

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Levantar PostgreSQL
Write-Host "[1/3] Levantando PostgreSQL..." -ForegroundColor Cyan
docker-compose up -d

# Esperar PostgreSQL
Start-Sleep -Seconds 5

# Levantar Backend en background
Write-Host "[2/3] Levantando Backend..." -ForegroundColor Cyan
$Env:NG_CLI_ANALYTICS = "false"
Start-Process -FilePath "npx" -ArgumentList "ng","serve","--host","0.0.0.0","--disable-host-check" -WorkingDirectory (Join-Path $ScriptDir "frontend") -PassThru

# Iniciar backend
Start-Process -FilePath "cmd.exe" -ArgumentList "/c", ".\gradlew.bat :backend:bootRun" -WorkingDirectory $ScriptDir -PassThru

Write-Host "[3/3] Todos los servicios iniciando..." -ForegroundColor Cyan
Write-Host ""
Write-Host "Frontend: http://localhost:4200" -ForegroundColor Green
Write-Host "Backend:  http://localhost:8080" -ForegroundColor Green