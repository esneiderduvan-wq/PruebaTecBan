# Diagrama Entidad-Relación - Sistema de Ofertas BRP

## Vista General del Modelo

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   USUARIO                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ username (VARCHAR(50)) UNIQUE                                                   │
│ password (VARCHAR(255)) - BCrypt                                                │
│ email (VARCHAR(255)) UNIQUE                                                     │
│ rol (VARCHAR(30)) - ADMINISTRADOR / RADICADOR / ANALISTA / JEFE                │
│ activo (BOOLEAN) DEFAULT TRUE                                                   │
│ fecha_creacion (TIMESTAMP)                                                      │
│ ultimo_acceso (TIMESTAMP) [NULL]                                                │
│                                                                                 │
│ Módulos: Set<String> @ElementCollection (EAGER)                                 │
│   - ADMIN: CATALOGO, SEGUIMIENTO, ADMINISTRACION                                │
│   - RADICADOR: CATALOGO, RADICACION, SEGUIMIENTO                                │
│   - ANALISTA: ANALISIS, CATALOGO, SEGUIMIENTO                                   │
│   - JEFE: APROBACION, CATALOGO, SEGUIMIENTO                                     │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
            ▼                       ▼                       ▼
    ┌───────────────┐      ┌───────────────┐      ┌───────────────┐
    │ Crea OFERTAS  │      │  Analista de  │      │   Aprueba/    │
    │  (RADICADOR)  │      │   OFERTAS     │      │  Devuelve     │
    │               │      │  (ANALISTA)   │      │    (JEFE)     │
    └───────────────┘      └───────────────┘      └───────────────┘
```

---

## Entidades Principales

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   BRP (Catálogo)                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: codigo (VARCHAR(50))                                                        │
│ estado (VARCHAR(30))                                                            │
│     DISPONIBLE / SIN_PRECIO / PENDIENTE_TECNICO / EN_TRAMITE_DE_VENTA /        │
│     SANEAMIENTO / DADO_DE_BAJA                                                  │
│ tipo_brp (VARCHAR(30)) - APARTAMENTO / CASA / LOCAL / LOTE / VEHICULO /        │
│     MAQUINARIA                                                                  │
│ matricula_inmiaria (VARCHAR(50)) [NULL] - Solo inmuebles                        │
│ placa (VARCHAR(20)) [NULL] - Solo vehículos                                     │
│ departamento (VARCHAR(100))                                                     │
│ municipio (VARCHAR(100))                                                        │
│ descripcion (TEXT)                                                              │
│ precio (DECIMAL(15,2)) [NULL]                                                   │
│ fecha_asignacion_precio (TIMESTAMP) [NULL]                                      │
│ anio_ingesta (INTEGER)                                                          │
│ mes_ingesta (INTEGER)                                                           │
│ dia_ingesta (INTEGER)                                                           │
│ fecha_modificacion (TIMESTAMP) [NULL]                                           │
│                                                                                 │
│ CHECK: (matricula_inmiaria IS NULL OR placa IS NULL)                            │
│ CHECK: (estado = 'SIN_PRECIO' AND precio IS NULL) OR                            │
│        (estado != 'SIN_PRECIO' AND precio IS NOT NULL)                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1:1 (una oferta activa por BRP)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   OFERTA                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: brp_codigo (VARCHAR(50)) → brp.codigo                                       │
│ FK: radicado_por_id (BIGINT) → usuario.id                                       │
│ FK: analista_id (BIGINT) → usuario.id [NULL]                                    │
│ valor_total (DECIMAL(15,2)) - Suma de todos los pagos                           │
│ descuento (DECIMAL(10,4)) [NULL] - % (precio - valor) / precio                  │
│ estado (VARCHAR(30))                                                            │
│     RADICADO → EN_PROCESO → ESCALADO → [APROBADO / RECHAZADO]                   │
│ version (BIGINT) - @Version para control de concurrencia                        │
│ fecha_creacion (TIMESTAMP)                                                      │
│ fecha_modificacion (TIMESTAMP) [NULL]                                           │
│ fecha_escalamiento (TIMESTAMP) [NULL]                                           │
│ fecha_devolucion (TIMESTAMP) [NULL]                                             │
│ fecha_finalizacion (TIMESTAMP) [NULL]                                           │
│ motivo_devolucion (VARCHAR(1000)) [NULL]                                        │
│                                                                                 │
│ UNIQUE: brp_codigo (solo una oferta activa por BRP)                             │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Entidades Hijas de Oferta

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   OFERENTE                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: oferta_id (BIGINT) → oferta.id (CASCADE ALL)                               │
│ tipo_identificacion (VARCHAR(20)) - CC / NIT                                    │
│ numero_identificacion (VARCHAR(20))                                             │
│ nombre (VARCHAR(255))                                                           │
│ porcentaje_participacion (DECIMAL(5,2)) - Calculado automático                  │
│ email (VARCHAR(255)) [NULL]                                                     │
│ telefono (VARCHAR(20)) [NULL]                                                   │
│                                                                                 │
│ Relación: 1 OFERTA → 1..5 OFERENTES                                             │
│ CHECK: porcentaje_participacion > 0 AND porcentaje_participacion <= 100         │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1:N (un oferente puede tener varios pagos)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                     PAGO                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: oferente_id (BIGINT) → oferente.id (CASCADE ALL)                           │
│ tipo_pago (VARCHAR(20)) - CONTADO / PRESTAMO                                    │
│ fecha (DATE)                                                                    │
│ valor (DECIMAL(15,2))                                                           │
│ entidad_financiadora (VARCHAR(100)) [NULL] - Solo si PRESTAMO                   │
│                                                                                 │
│ Relación: 1 OFERENTE → 1..6 PAGOS                                               │
│ CHECK: valor > 0                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   ADJUNTO                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PK: id (BIGINT)                                                                 │
│ FK: oferta_id (BIGINT) → oferta.id (CASCADE ALL)                               │
│ nombre_archivo (VARCHAR(255))                                                   │
│ ruta_local (VARCHAR(500)) - backend/uploads/adjuntos/{timestamp}_{nombre}       │
│ descripcion (VARCHAR(255)) [NULL]                                               │
│ fecha_carga (TIMESTAMP) - DEFAULT CURRENT_TIMESTAMP                             │
│                                                                                 │
│ Relación: 1 OFERTA → 0..N ADJUNTOS                                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Flujo de Transiciones de Estado

```
┌──────────────┐
│    BRP       │ Estado inicial: DISPONIBLE (con precio)
│  CATÁLOGO    │ Si no tiene precio: SIN_PRECIO
└──────┬───────┘
       │
       │ Oferta.radicarOferta()
       ▼
┌─────────────────┐
│     RADICADO    │ Esperando que un ANALISTA tome la oferta
│  (Nueva oferta) │
└────────┬────────┘
         │
         │ POST /{id}/tomar (ANALISTA)
         ▼
┌─────────────────┐
│   EN_PROCESO    │ El ANALISTA está analizando
│                 │ - Puede ESCALAR a aprobación
│                 │ - Puede RECHAZAR (vuelve a DISPONIBLE)
└────────┬────────┘
         │
         │ POST /{id}/escalar (ANALISTA)
         ▼
┌─────────────────┐
│    ESCALADO     │ Esperando decisión del JEFE
│                 │ - Puede APROBAR (estado final)
│                 │ - Puede RECHAZAR (vuelve a DISPONIBLE)
│                 │ - Puede DEVOLVER a EN_PROCESO
└────────┬────────┘
         │
         ├──────────────────┐
         │                  │
         ▼                  ▼
┌─────────────────┐  ┌─────────────────┐
│    APROBADO     │  │   RECHAZADO     │
│   (FINAL)       │  │    (FINAL)      │
│                 │  │ BRP → DISPONIBLE│
└─────────────────┘  └─────────────────┘
```

---

## Modelo de Seguridad por Roles

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              ROLES Y PERMISOS                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ADMINISTRADOR                                                                 │
│  ├── Ver CATÁLOGO completo                                                     │
│  ├── Ver SEGUIMIENTO de todas las ofertas                                      │
│  ├── ADMINISTRACIÓN de usuarios                                                │
│  └── Crear usuarios y asignar módulos                                          │
│                                                                                 │
│  RADICADOR                                                                     │
│  ├── Ver CATÁLOGO (solo DISPONIBLES con precio)                                │
│  ├── Ver SEGUIMIENTO de sus propias ofertas                                    │
│  ├── RADICAR nuevas ofertas                                                    │
│  └── Ver adjuntos de sus ofertas                                               │
│                                                                                 │
│  ANALISTA                                                                      │
│  ├── Ver SEGUIMIENTO de ofertas asignadas                                      │
│  ├── ANALISIS de ofertas tomadas                                               │
│  ├── Tomar ofertas RADICADAS                                                   │
│  ├── Escalar a aprobación                                                      │
│  ├── Rechazar ofertas                                                          │
│  └── Ver/subir adjuntos                                                        │
│                                                                                 │
│  JEFE                                                                          │
│  ├── Ver SEGUIMIENTO de todas las ofertas                                      │
│  ├── APROBACIÓN de ofertas escaladas                                           │
│  ├── Devolver ofertas al analista                                              │
│  ├── Rechazar ofertas                                                          │
│  └── Ver historial de cambios                                                  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Resumen de Relaciones

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              RESUMEN DE RELACIONES                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  USUARIO (1) ───┐                                                              │
│                 │── (N) OFERTA: Creadas por RADICADOR/ADMINISTRADOR             │
│                 │── (N) OFERTA: Asignadas a ANALISTA                            │
│                                                                                 │
│  BRP (1) ─── (1) OFERTA: Una oferta activa por BRP                             │
│                                                                                 │
│  OFERTA (1) ─── (N) OFERENTE: 1 a 5 oferentes                                  │
│                 ─── (N) ADJUNTO: 0 a N adjuntos                                 │
│                                                                                 │
│  OFERENTE (1) ── (N) PAGO: 1 a 6 pagos                                         │
│                                                                                 │
│  USUARIO ─── Set<String> MODULOS: @ElementCollection (EAGER)                   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Credenciales de Prueba

| Username | Password | Rol | Módulos |
|----------|----------|-----|---------|
| admin | admin123 | ADMINISTRADOR | CATALOGO, SEGUIMIENTO, ADMINISTRACION |
| radicador1 | admin123 | RADICADOR | CATALOGO, RADICACION, SEGUIMIENTO |
| analista1 | admin123 | ANALISTA | ANALISIS, SEGUIMIENTO |
| jefe1 | admin123 | JEFE | APROBACION, SEGUIMIENTO |

---

## Tecnologías del Stack

| Capa | Tecnología |
|------|------------|
| Backend | Spring Boot 3.2.0, Java 17, Spring Security 6, JWT |
| Persistencia | JPA/Hibernate, PostgreSQL 16, Liquibase |
| Frontend | Angular 17 (standalone components) |
| Build | Gradle 8.5 |
| Storage | Sistema de archivos local (`uploads/adjuntos/`) |
| Testing | 29 tests unitarios pasando |