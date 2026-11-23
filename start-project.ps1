#!/usr/bin/env pwsh
# ============================================================================
# Script para levantar el proyecto completo: PostgreSQL + Backend + Frontend
# ============================================================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  BRP - Sistema de Gestión de Ofertas" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Colors for output
$Green = [System.ConsoleColor]::Green
$Yellow = [System.ConsoleColor]::Yellow
$Red = [System.ConsoleColor]::Red
$Cyan = [System.ConsoleColor]::Cyan

function Write-Step {
    param([string]$Message)
    Write-Host "[PASO] " -ForegroundColor $Cyan -NoNewline
    Write-Host $Message -ForegroundColor White
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] " -ForegroundColor $Green -NoNewline
    Write-Host $Message -ForegroundColor White
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] " -ForegroundColor $Yellow -NoNewline
    Write-Host $Message -ForegroundColor White
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] " -ForegroundColor $Red -NoNewline
    Write-Host $Message -ForegroundColor White
}

# ============================================================================
# DEFINICIÓN DE RUTAS
# ============================================================================
$BackendDir = Join-Path $ScriptDir "backend"
$FrontendDir = Join-Path $ScriptDir "frontend"
$ChangelogDir = Join-Path $BackendDir "src\main\resources\db\changelog"
$CsvPath = Join-Path $ScriptDir "brp_ejemplo.csv"
$MigrationCsvPath = Join-Path $ChangelogDir "008-seed-brp-from-csv.xml"

# ============================================================================
# 1. GENERAR MIGRACIÓN DESDE CSV (si no existe)
# ============================================================================
Write-Step "Verificando migración de BRPs desde CSV..."

if (-not (Test-Path $CsvPath)) {
    Write-Warn "No se encontró brp_ejemplo.csv, saltando generación de migración"
} else {
    if (-not (Test-Path $MigrationCsvPath)) {
        Write-Step "Generando migración 008-seed-brp-from-csv.xml desde CSV..."
        
        try {
            $GenerateScript = Join-Path $BackendDir "generate-liquibase.ps1"
            if (Test-Path $GenerateScript) {
                & $GenerateScript | Out-Null
                Write-Success "Migración generada: 008-seed-brp-from-csv.xml"
            } else {
                Write-Warn "Script de generación no encontrado"
            }
        } catch {
            Write-Warn "Error al generar migración: $($_.Exception.Message)"
        }
    } else {
        Write-Success "Migración ya existe: 008-seed-brp-from-csv.xml"
    }
}

# ============================================================================
# 2. LEVANTAR POSTGRESQL CON DOCKER
# ============================================================================
Write-Step "Verificando estado de PostgreSQL..."

$DockerComposePath = Join-Path $ScriptDir "docker-compose.yml"

if (-not (Test-Path $DockerComposePath)) {
    Write-Error "No se encontró docker-compose.yml en la carpeta del proyecto"
    exit 1
}

# Check if containers are already running
$ContainersRunning = docker ps --filter "name=brp-postgres" --format "{{.Names}}" 2>$null

if ($ContainersRunning -contains "brp-postgres") {
    Write-Success "PostgreSQL ya está corriendo"
} else {
    Write-Step "Levantando PostgreSQL con Docker..."
    docker-compose up -d

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Error al levantar PostgreSQL"
        exit 1
    }

    Write-Success "Contenedor PostgreSQL iniciado"
    Write-Step "Esperando a que la base de datos esté lista..."
    
    $MaxRetries = 30
    $Retries = 0
    $Ready = $false

    while ($Retries -lt $MaxRetries) {
        try {
            $Result = docker exec brp-postgres pg_isready -U brp_user -d brp_db 2>&1
            if ($Result -match "accepting connections") {
                $Ready = $true
                break
            }
        } catch {}
        Start-Sleep -Seconds 1
        $Retries++
        Write-Host "." -ForegroundColor $Cyan -NoNewline
    }
    Write-Host ""

    if (-not $Ready) {
        Write-Warn "No se pudo verificar que PostgreSQL esté listo, continuando..."
    } else {
        Write-Success "PostgreSQL está listo y aceptando conexiones"
    }
}

# ============================================================================
# 3. LEVANTAR BACKEND
# ============================================================================
Write-Host ""
Write-Step "Iniciando Backend (Spring Boot)..."

if (-not (Test-Path $BackendDir)) {
    Write-Error "No se encontró la carpeta backend"
    exit 1
}

# Check if backend is already running
try {
    $Response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($Response.StatusCode -eq 200) {
        Write-Success "Backend ya está corriendo en puerto 8080"
    }
} catch {
    # Backend no está corriendo, lo iniciamos
    Write-Step "Compilando y ejecutando Backend..."

    $BackendBuild = & "$ScriptDir\gradlew.bat" :backend:bootRun 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Error al iniciar el Backend"
        Write-Host $BackendBuild -ForegroundColor Red
        exit 1
    }
}

# ============================================================================
# 4. LEVANTAR FRONTEND
# ============================================================================
Write-Host ""
Write-Step "Iniciando Frontend (Angular)..."

if (-not (Test-Path $FrontendDir)) {
    Write-Error "No se encontró la carpeta frontend"
    exit 1
}

# Check if frontend is already running
try {
    $Response = Invoke-WebRequest -Uri "http://localhost:4200" -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($Response.StatusCode -eq 200) {
        Write-Success "Frontend ya está corriendo en puerto 4200"
    }
} catch {
    # Frontend no está corriendo, lo iniciamos
    Write-Step "Ejecutando Frontend..."

    # Disable Angular analytics
    $Env:NG_CLI_ANALYTICS = "false"

    # Start frontend in background
    $FrontendProcess = Start-Process -FilePath "npx" -ArgumentList "ng","serve","--host","0.0.0.0","--disable-host-check" -WorkingDirectory $FrontendDir -PassThru -NoNewWindow

    Write-Success "Frontend iniciando en http://localhost:4200"
}

# ============================================================================
# RESUMEN FINAL
# ============================================================================
Write-Host ""
Write-Host "============================================" -ForegroundColor $Green
Write-Host "  ✅ Proyecto iniciado correctamente" -ForegroundColor $Green
Write-Host "============================================" -ForegroundColor $Green
Write-Host ""
Write-Host "Flujo de inicio:" -ForegroundColor White
Write-Host "  1. Generar migración 008-seed-brp-from-csv.xml" -ForegroundColor $Cyan
Write-Host "  2. Levantar PostgreSQL" -ForegroundColor $Cyan
Write-Host "  3. Levantar Backend (Liquibase ejecuta migración)" -ForegroundColor $Cyan
Write-Host "  4. Levantar Frontend" -ForegroundColor $Cyan
Write-Host ""
Write-Host "Servicios disponibles:" -ForegroundColor White
Write-Host "  • PostgreSQL: localhost:5432 (brp_db/brp_user)" -ForegroundColor $Yellow
Write-Host "  • Backend API: http://localhost:8080" -ForegroundColor $Yellow
Write-Host "  • Frontend: http://localhost:4200" -ForegroundColor $Yellow
Write-Host ""
Write-Host "Credenciales de prueba:" -ForegroundColor White
Write-Host "  • admin / admin123 (ADMINISTRADOR)" -ForegroundColor $Yellow
Write-Host "  • radicador1 / admin123 (RADICADOR)" -ForegroundColor $Yellow
Write-Host "  • analista1 / admin123 (ANALISTA)" -ForegroundColor $Yellow
Write-Host "  • jefe1 / admin123 (JEFE)" -ForegroundColor $Yellow
Write-Host ""
Write-Host "Documentación API:" -ForegroundColor White
Write-Host "  • README.md en la raíz del proyecto" -ForegroundColor $Yellow
Write-Host ""
Write-Host "Presiona Ctrl+C para detener todos los servicios" -ForegroundColor $Cyan

# Keep script running
try {
    while ($true) {
        Start-Sleep -Seconds 5
    }
} finally {
    Write-Host ""
    Write-Step "Deteniendo servicios..."
    docker-compose down
    Write-Success "Contenedores de Docker detenido"
}