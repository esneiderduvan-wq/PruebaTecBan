# Diagrama Entidad-Relación (BRP System)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   BRP (Catálogo)                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: codigo (VARCHAR(50))                                                        │
│ estado (VARCHAR(30))                                                            │
│ tipo_brp (VARCHAR(30))                                                          │
│ matricula_inmobiliaria (VARCHAR(50)) [NULL]                                     │
│ placa (VARCHAR(20)) [NULL]                                                      │
│ departamento (VARCHAR(100))                                                     │
│ municipio (VARCHAR(100))                                                        │
│ descripcion (TEXT)                                                              │
│ precio (DECIMAL(15,2)) [NULL]                                                   │
│ fecha_asignacion_precio (DATE) [NULL]                                           │
│ anio_ingesta (INTEGER)                                                          │
│ mes_ingesta (INTEGER)                                                           │
│ dia_ingesta (INTEGER)                                                           │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1:1 (única oferta activa)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   OFERTA                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: brp_codigo (VARCHAR(50)) → brp.codigo                                       │
│ FK: radicado_por_id (BIGINT) → usuario.id                                       │
│ FK: analista_id (BIGINT) → usuario.id [NULL]                                    │
│ valor_total (DECIMAL(15,2))                                                     │
│ descuento (DECIMAL(10,4)) [NULL]                                                │
│ estado (VARCHAR(30))                                                            │
│   RADICADO → EN_PROCESO → ESCALADO → [APROBADO / RECHAZADO]                     │
│   o RADICADO → EN_PROCESO → RECHAZADO                                           │
│ version (BIGINT)                                                                │
│ fecha_creacion (TIMESTAMP)                                                      │
│ fecha_modificacion (TIMESTAMP) [NULL]                                           │
│ fecha_escalamiento (TIMESTAMP) [NULL]                                           │
│ fecha_devolucion (TIMESTAMP) [NULL]                                             │
│ motivo_devolucion (VARCHAR(1000)) [NULL]                                        │
│ fecha_finalizacion (TIMESTAMP) [NULL]                                           │
│                                                                                 │
│ UNIQ: brp_codigo (una oferta activa por BRP)                                    │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
            ▼                       ▼                       ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│       OFERENTE        │ │       PAGO            │ │       ADJUNTO         │
├───────────────────────┤ ├───────────────────────┤ ├───────────────────────┤
│ PK: id (BIGINT)       │ │ PK: id (BIGINT)       │ │ PK: id (BIGINT)       │
│ FK: oferta_id →       │ │ FK: oferente_id →     │ │ FK: oferta_id →       │
│   oferta.id (CASCADE) │ │ oferente.id (CASCADE) │ │   oferta.id (CASCADE) │
│ tipo_identificacion   │ │ tipo_pago             │ │ nombre_archivo        │
│ numero_identificacion │ │ fecha (DATE)          │ │ ruta_local            │
│ nombre                │ │ valor (DECIMAL)       │ │ fecha_carga           │
│ porcentaje_particip   │ │ entidad_financiadora  │ │                       │
│   (DECIMAL 5,2)       │ │                       │ │                       │
│ email                 │ │                       │ │                       │
│ telefono              │ │                       │ │                       │
│                       │ │                       │ │                       │
│ CHECK: porcentaje     │ │ CHECK: valor > 0      │ │                       │
│   > 0 AND <= 100      │ │                       │ │                       │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   USUARIO                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ username (VARCHAR(50)) UNIQUE                                                   │
│ password (VARCHAR(255)) BCrypt                                                  │
│ nombre (VARCHAR(100))                                                           │
│ apellido (VARCHAR(100))                                                         │
│ email (VARCHAR(255))                                                            │
│ activo (BOOLEAN)                                                                │
│ rol (VARCHAR(30))                                                               │
│   ADMINISTRADOR / RADICADOR / ANALISTA / JEFE                                   │
│ created_at (TIMESTAMP)                                                          │
│ updated_at (TIMESTAMP) [NULL]                                                   │
│ created_by (BIGINT) [NULL] → usuario.id                                         │
│ updated_by (BIGINT) [NULL] → usuario.id                                         │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1:N
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                 USUARIO_MODULO                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: usuario_id → usuario.id                                                     │
│ FK: modulo_id → modulo.id                                                       │
│ FK: asignado_por → usuario.id [NULL]                                            │
│ fecha_asignacion (TIMESTAMP)                                                    │
│                                                                                 │
│ UNIQ: (usuario_id, modulo_id)                                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   MODULO                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ nombre (VARCHAR(50)) UNIQUE                                                     │
│   RADICACION / ANALISIS / APROBACION / CATALOGO / SEGUIMIENTO / ADMINISTRACION  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                               AUDITORIA_MODULO                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: usuario_id → usuario.id                                                     │
│ FK: asignado_por → usuario.id [NULL]                                            │
│ modulo_nombre (VARCHAR(50))                                                     │
│ accion (VARCHAR(20))                                                            │
│   ASIGNAR / REMOVER                                                             │
│ fecha (TIMESTAMP) default CURRENT_TIMESTAMP                                     │
└─────────────────────────────────────────────────────────────────────────────────┘


```

## Restricciones de Negocio (Checks)

| Tabla | Constraint | Descripción |
|-------|------------|-------------|
| `brp` | chk_brp_fecha_precio | fecha_asignacion_precio y precio NULL o NOT NULL juntos |
| `brp` | chk_brp_matricula_placa | matricula_inmobiliaria y placa no pueden ser ambas NOT NULL |
| `oferente` | chk_oferente_porcentaje | porcentaje_participación > 0 y <= 100 |
| `pago` | chk_pago_valor | valor > 0 |
| `oferta` | chk_oferta_valor_total | valor_total > 0 |

## Estados de Oferta

```
RADICADO
    ↓ (tomar)
EN_PROCESO
    ├─→ (rechazar) → RECHAZADO
    └─→ (escalar) → ESCALADO
                      ├─→ (aprobar) → APROBADO
                      ├─→ (rechazar) → RECHAZADO
                      └─→ (devolver) → EN_PROCESO
```

## Credenciales de Prueba (BCrypt)

| Username | Password | Rol | Módulos asignados |
|----------|----------|-----|-------------------|
| admin | admin123 | ADMINISTRADOR | CATALOGO, SEGUIMIENTO, ADMINISTRACION |
| radicador1 | admin123 | RADICADOR | RADICACION, CATALOGO, SEGUIMIENTO |
| analista1 | admin123 | ANALISTA | ANALISIS, CATALOGO, SEGUIMIENTO |
| jefe1 | admin123 | JEFE | APROBACION, CATALOGO, SEGUIMIENTO |
