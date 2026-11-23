# Script para generar XML de Liquibase desde CSV
# Uso: .\generate-liquibase.ps1

$csvPath = "g:\DUVAN\Descargas\Prueba tecnica Ban\brp_ejemplo.csv"
$outputPath = "g:\DUVAN\Descargas\Prueba tecnica Ban\backend\src\main\resources\db\changelog\008-seed-brp-from-csv.xml"

# Leer CSV
$csv = Import-Csv $csvPath

# Contador para changesets (25 registros por changeset)
$registrosPorChangeset = 25
$changesetNum = 1
$contador = 0
$totalRegistros = ($csv | Measure-Object).Count

# Inicio del XML
$xmlContent = "<?xml version=""1.0"" encoding=""UTF-8""?>`n"
$xmlContent += "<databaseChangeLog`n"
$xmlContent += "    xmlns=""http://www.liquibase.org/xml/ns/dbchangelog""`n"
$xmlContent += "    xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""`n"
$xmlContent += "    xsi:schemaLocation=""http://www.liquibase.org/xml/ns/dbchangelog`n"
$xmlContent += "                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd"">`n`n"

$xmlContent += "<changeSet id='008-$changesetNum' author='brp'>`n"

foreach ($row in $csv) {
    $contador++
    
    # Sanitizar valores
    $codigo = $row.codigo
    $estado = $row.estado
    $tipoBrp = $row.tipo_brp.ToUpper()
    $matricula = if ([string]::IsNullOrEmpty($row.matricula_inmiaria)) { "" } else { $row.matricula_inmiaria }
    $placa = if ([string]::IsNullOrEmpty($row.placa)) { "" } else { $row.placa }
    $departamento = $row.departamento
    $municipio = $row.municipio
    $descripcion = $row.descripcion.Replace('"', "'").Replace("&", "and")
    $precio = $row.precio
    $fechaAsignacion = if ([string]::IsNullOrEmpty($row.fecha_asignacion_precio)) { "" } else { $row.fecha_asignacion_precio }
    $anioIngesta = $row.anio_ingesta
    $mesIngesta = $row.mes_ingesta
    $diaIngesta = $row.dia_ingesta

    # Generar insert
    $xmlContent += "    <!-- $($row.codigo) -->`n"
    $xmlContent += "    <insert tableName='brp'>`n"
    $xmlContent += "        <column name='codigo'>$codigo</column>`n"
    $xmlContent += "        <column name='estado'>$estado</column>`n"
    $xmlContent += "        <column name='tipo_brp'>$tipoBrp</column>`n"
    if ($matricula) { $xmlContent += "        <column name='matricula_inmobiliaria'>$matricula</column>`n" }
    if ($placa) { $xmlContent += "        <column name='placa'>$placa</column>`n" }
    $xmlContent += "        <column name='departamento'>$departamento</column>`n"
    $xmlContent += "        <column name='municipio'>$municipio</column>`n"
    $xmlContent += "        <column name='descripcion'>$descripcion</column>`n"
    $xmlContent += "        <column name='precio'>$precio</column>`n"
    if ($fechaAsignacion) { $xmlContent += "        <column name='fecha_asignacion_precio'>$fechaAsignacion</column>`n" }
    $xmlContent += "        <column name='anio_ingesta'>$anioIngesta</column>`n"
    $xmlContent += "        <column name='mes_ingesta'>$mesIngesta</column>`n"
    $xmlContent += "        <column name='dia_ingesta'>$diaIngesta</column>`n"
    $xmlContent += "    </insert>`n"

    # Nuevo changeset cada 25 registros Y quedan más registros
    if ($contador % $registrosPorChangeset -eq 0 -and $contador -lt $totalRegistros) {
        $xmlContent += "</changeSet>`n`n"
        $changesetNum++
        $xmlContent += "<changeSet id='008-$changesetNum' author='brp'>`n"
    }
}

# Cerrar último changeset
$xmlContent += "</changeSet>`n"

# Cerrar XML
$xmlContent += "</databaseChangeLog>"

# Escribir archivo
$xmlContent | Out-File -Encoding UTF8 $outputPath

Write-Host "Generado: $outputPath"
Write-Host "Total registros: $contador"
Write-Host "Changesets: $changesetNum"