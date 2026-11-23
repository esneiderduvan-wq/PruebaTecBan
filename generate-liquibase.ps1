#!/usr/bin/env pwsh
# Script para generar archivo Liquibase desde CSV

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CsvPath = Join-Path $ScriptDir "brp_ejemplo.csv"
$OutputPath = Join-Path $ScriptDir "backend\src\main\resources\db\changelog\007-seed-brp-complete.xml"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  GENERAR LIQUIBASE DESDE CSV" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Read CSV
Write-Host "Leyendo CSV..." -ForegroundColor Yellow
$CsvContent = Import-Csv -Path $CsvPath -Encoding UTF8
Write-Host "Total de registros: $($CsvContent.Count)" -ForegroundColor Green
Write-Host ""

# Start XML
$xml = @('<?xml version="1.0" encoding="UTF-8"?>')
$xml += '<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"'
$xml += ' xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"'
$xml += ' xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog'
$xml += ' http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">'
$xml += ''

# Process each row
$counter = 0
$currentChangeSetId = 1

foreach ($row in $CsvContent) {
    $counter++
    
    # Start new changeset every 20 records
    if (($counter - 1) % 20 -eq 0) {
        if ($counter -gt 1) {
            $xml += '</changeSet>'
            $xml += ''
        }
        $xml += "<changeSet id='007-$currentChangeSetId' author='brp'>"
        $currentChangeSetId++
    }
    
    # Handle nulls
    $matricula = if ($row.matricula_inmobiliaria) { $row.matricula_inmobiliaria } else { "" }
    $placa = if ($row.placa) { $row.placa } else { "" }
    $precio = if ($row.precio) { $row.precio } else { "" }
    $fecha = if ($row.fecha_asignacion_precio) { $row.fecha_asignacion_precio } else { "" }
    
    # Escape XML special characters
    $descripcion = $row.descripcion -replace '&', '&amp;' -replace '<', '&lt;' -replace '>', '&gt;'
    
    $xml += "    <!-- $($row.codigo) -->"
    $xml += "    <insert tableName='brp'>"
    $xml += "        <column name='codigo'>$($row.codigo)</column>"
    $xml += "        <column name='estado'>$($row.estado)</column>"
    $xml += "        <column name='tipo_brp'>$($row.tipo_brp)</column>"
    
    if ($matricula) {
        $xml += "        <column name='matricula_inmobiliaria'>$matricula</column>"
    } else {
        $xml += "        <column name='matricula_inmobiliaria' />"
    }
    
    if ($placa) {
        $xml += "        <column name='placa'>$placa</column>"
    } else {
        $xml += "        <column name='placa' />"
    }
    
    $xml += "        <column name='departamento'>$($row.departamento)</column>"
    $xml += "        <column name='municipio'>$($row.municipio)</column>"
    $xml += "        <column name='descripcion'>$descripcion</column>"
    
    if ($precio) {
        $xml += "        <column name='precio'>$precio</column>"
    } else {
        $xml += "        <column name='precio' />"
    }
    
    if ($fecha) {
        $xml += "        <column name='fecha_asignacion_precio'>$fecha</column>"
    } else {
        $xml += "        <column name='fecha_asignacion_precio' />"
    }
    
    $xml += "        <column name='anio_ingesta'>$($row.anio_ingesta)</column>"
    $xml += "        <column name='mes_ingesta'>$($row.mes_ingesta)</column>"
    $xml += "        <column name='dia_ingesta'>$($row.dia_ingesta)</column>"
    $xml += "    </insert>"
}

# Close last changeset
$xml += '</changeSet>'
$xml += ''
$xml += '</databaseChangeLog>'

# Write to file
$xml | Set-Content -Path $OutputPath -Encoding UTF8

Write-Host "============================================" -ForegroundColor Green
Write-Host "  ARCHIVO GENERADO: $OutputPath" -ForegroundColor Green
Write-Host "  Total de registros: $counter" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Importante: Actualizar master.xml para incluir este archivo" -ForegroundColor Yellow