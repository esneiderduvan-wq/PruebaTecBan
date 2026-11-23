@echo off
REM ============================================================================
REM Script para levantar el proyecto BRP completo
REM ============================================================================

echo.
echo ============================================
echo   BRP - Sistema de Gestion de Ofertas
echo ============================================
echo.

REM Cambiar al directorio del script
cd /d "%~dp0"

REM 1. Levantar PostgreSQL
echo [1/3] Levantando PostgreSQL con Docker...
docker-compose up -d
if errorlevel 1 (
    echo ERROR: No se pudo levantar PostgreSQL
    exit /b 1
)
echo OK: PostgreSQL iniciado

REM Esperar a que la base de datos este lista
echo Esperando a PostgreSQL...
timeout /t 10 /nobreak >nul

REM 2. Levantar Backend
echo [2/3] Levantando Backend (Spring Boot)...
start "BRP Backend" cmd /c ".\gradlew.bat :backend:bootRun"
echo OK: Backend iniciando...

REM 3. Levantar Frontend
echo [3/3] Levantando Frontend (Angular)...
set NG_CLI_ANALYTICS=false
start "BRP Frontend" cmd /c "cd frontend && npx ng serve --host 0.0.0.0 --disable-host-check"
echo OK: Frontend iniciando...

echo.
echo ============================================
echo   Proyecto iniciado correctamente
echo ============================================
echo.
echo Servicios:
echo   - PostgreSQL: localhost:5432
echo   - Backend API: http://localhost:8080
echo   - Frontend:    http://localhost:4200
echo.
echo Credenciales:
echo   - admin/admin123 (ADMIN)
echo   - radicador1/admin123 (RADICADOR)
echo   - analista1/admin123 (ANALISTA)
echo   - jefe1/admin123 (JEFE)
echo.
echo Presiona Ctrl+C para detener todos los servicios
echo.
pause