# Sistema de Ofertas BRP

Sistema para gestionar ofertas sobre Bienes Recibidos en Pago (BRP) desarrollado como prueba técnica para Bancolombia.

---

## 🚀 Inicio Rápido (3 comandos)

```powershell
# 1. Levantar todo automáticamente
.\start-project.ps1

# Listo! Los 3 servicios estarán corriendo:
# - PostgreSQL: localhost:5432
# - Backend API: http://localhost:8080
# - Frontend: http://localhost:4200
```

**Credenciales:** admin/admin123, radicador1/admin123, analista1/admin123, jefe1/admin123in123

### Inicio Manual (si prefieres paso a paso)

```powershell
# 1. Levantar PostgreSQL
docker-compose up -d

# 2. Backend
.\gradlew.bat :backend:bootRun

# 3. Frontend (en otra terminal)
cd frontend
npm start
```

## Stack Tecnológico

| Capa | Recomendación | Implementado |
| :- | :- | :- |
| JDK | Java 21 o Java 25 | OpenJDK 17 |
| Backend | Spring Boot, Spring Data JPA, Spring Security, Bean Validation | ✅ Spring Boot 3.2.0 |
| Frontend | Angular 17+ | ✅ Angular 17 (standalone) |
| Base de datos | PostgreSQL con Docker Compose | ✅ PostgreSQL 16 |
| Migraciones | Liquibase versionado | ✅ Liquibase con 100 BRP desde CSV |
| Adjuntos | Almacenamiento local en disco | ✅ `uploads/adjuntos/` |
| Seguridad | Login propio, BCrypt, JWT | ✅ BCrypt + JWT 8h |
| Build | Gradle 8+ | ✅ Gradle 8.5 |
| Pruebas | JUnit unitarias | ✅ 29 tests, 29 pasan |

## Requisitos Previos

- Docker y Docker Compose
- Java 17 o superior
- Git

## 📥 Importar Catálogo BRP desde CSV

### Carga Inicial (Automática)
El catálogo con 100 BRP se carga **automáticamente** al levantar el proyecto:

```powershell
.\start-project.ps1
# Flujo:
# 1. Genera 008-seed-brp-from-csv.xml desde brp_ejemplo.csv
# 2. Levanta PostgreSQL
# 3. Levanta Backend (Liquibase ejecuta la migración)
# 4. Levanta Frontend
```

### Generación Manual del XML

Si necesitas regenerar el XML de migración:

```powershell
cd backend
.\generate-liquibase.ps1
```

Esto genera `src/main/resources/db/changelog/008-seed-brp-from-csv.xml` con 100 BRPs en 4 changesets.

## API Endpoints

### Autenticación

```bash
# Login
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

# Respuesta
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 28800
}
```

### Catálogo BRP

```bash
# Listar todos los BRP
GET /api/brp
Authorization: Bearer <token>

# Listar solo disponibles (para radicar ofertas)
GET /api/brp/disponibles
Authorization: Bearer <token>

# Listar todos con historial (admin)
GET /api/brp/todos
Authorization: Bearer <token>

# Información del catálogo
GET /api/brp/catalogo/ultima-actualizacion
Authorization: Bearer <token>

# Historial de un BRP específico
GET /api/brp/BRP-0001/historial
Authorization: Bearer <token>
```

### Ofertas

```bash
# Listar ofertas (filtrado por rol)
GET /api/ofertas
Authorization: Bearer <token>

# Ver oferta específica
GET /api/ofertas/{id}
Authorization: Bearer <token>

# Radicar oferta (RADICADOR)
POST /api/ofertas
Authorization: Bearer <token>
Content-Type: application/json

{
  "brpCodigo": "BRP-0001",
  "oferentes": [
    {
      "tipoIdentificacion": "CC",
      "numeroIdentificacion": "12345678",
      "nombre": "Juan Pérez",
      "porcentajeParticipacion": 100.00,
      "email": "juan@email.com",
      "pagos": [
        {
          "tipoPago": "CONTADO",
          "fecha": "2026-11-20",
          "valor": 320000000
        }
      ]
    }
  ]
}

# Analista toma oferta
POST /api/ofertas/{id}/tomar

# Escalar al jefe (ANALISTA)
POST /api/ofertas/{id}/escalar

# Aprobar (JEFE)
POST /api/ofertas/{id}/aprobar

# Rechazar (ANALISTA o JEFE)
POST /api/ofertas/{id}/rechazar

# Devolver al analista (JEFE)
POST /api/ofertas/{id}/devolver
```

### Administración de Usuarios

```bash
# Listar usuarios
GET /api/usuarios

# Crear usuario (ADMIN)
POST /api/usuarios

# Asignar módulo
POST /api/usuarios/{id}/modulos?modulo=RADICACION

# Remover módulo
DELETE /api/usuarios/{id}/modulos?modulo=RADICACION
```

## Modelo de Datos

### Entidades Principales

- **Brp**: Catálogo de bienes (solo lectura)
- **Oferta**: Oferta sobre un BRP
- **Oferente**: Hasta 5 por oferta con participación calculada automáticamente
- **Pago**: Hasta 6 por oferente (contado/préstamo)
- **Adjunto**: Archivos adjuntos a ofertas (documentos PDF, DOC, etc.)
- **Usuario**: Usuarios del sistema con roles
- **Modulo**: Módulos de permisos (CATALOGO, SEGUIMIENTO, ADMINISTRACION, RADICACION, ANALISIS, APROBACION)
- **UsuarioModulo**: Relación usuario-módulo
- **AuditoriaModulo**: Historial de asignaciones de módulos a usuarios
- **MotivoDevolucion**: Registro de devoluciones de ofertas

### Estados de Oferta

```
RADICADO -> EN_PROCESO (analista toma)
EN_PROCESO -> ESCALADO (analista escala)
EN_PROCESO -> RECHAZADO (analista rechaza)
ESCALADO -> APROBADO (jefe aprueba)
ESCALADO -> RECHAZADO (jefe rechaza)
ESCALADO -> EN_PROCESO (jefe devuelve)
```

## Pruebas Unitarias

```bash
# Ejecutar pruebas
.\gradlew.bat :backend:test
```

## Archivos de Documentación Adicional

- **OTROS_ARCHIVOS.md** - Explicación de Lombok y MapStruct
- **SCRIPTS.md** - Guía de prueba del flujo completo

## Licencia

Este proyecto es una demo técnica para Bancolombia.