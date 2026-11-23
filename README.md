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

**Credenciales:** admin/admin123, radicador1/admin123, analista1/admin123, jefe1/admin123

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

| Capa | Tecnología |
|------|------------|
| JDK | OpenJDK 17 (compatible con Java 21+) |
| Backend | Spring Boot 3.2.0, Spring Data JPA, Spring Security |
| Base de datos | PostgreSQL 16 |
| Migraciones | Liquibase (100 BRP generados desde brp_ejemplo.csv) |
| Build | Gradle 8.5 |
| Frontend | Angular 17+ (standalone components) |

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

### Cargar BRPs adicionales (Admin)

```bash
# Descargar plantilla
curl http://localhost:8080/api/admin/brp/plantilla-csv -o plantilla_brp.csv

# Editar el archivo plantilla_brp.csv con tus datos

# Subir el CSV
curl -X POST http://localhost:8080/api/admin/brp/importar \
  -F "file=@ruta/a/tu_archivo.csv" \
  -H "Authorization: Bearer <token_admin>"

# Respuesta
{
  "totalRegistros": 100,
  "exitosos": 98,
  "fallidos": 2,
  "actualizaciones": 5,
  "mensajes": ["Fila 15: Estado inválido: ACTIVO"]
}
```

**Reglas de actualización:**
- Si el BRP ya existe (`codigo` duplicado), se actualiza con la nueva versión
- Si el BRP es nuevo, se crea
- El estado se ajusta automáticamente a `SIN_PRECIO` si `fecha_asignacion_precio` o `precio` son nulos
- Se mantiene el historial completo (no se borran versiones anteriores)

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
- **Oferente**: Hasta 5 por oferta
- **Pago**: Hasta 6 por oferente
- **Usuario**: Usuarios del sistema con roles
- **Modulo**: Módulos de permisos
- **UsuarioModulo**: Relación usuario-módulo
- **Adjunto**: Archivos adjuntos a ofertas

### Estados de Oferta

```
RADICADO -> EN_PROCESO (analista toma)
EN_PROCESO -> ESCALADO (analista escala)
EN_PROCESO -> RECHAZADO (analista rechaza)
ESCALADO -> APROBADO (jefe aprueba)
ESCALADO -> RECHAZADO (jefe rechaza)
ESCALADO -> EN_PROCESO (jefe devuelve)
```

## Respuestas a las Preguntas de la Sección 9

### 1. ¿Dónde vive la regla de transiciones de estado?

La validación de transiciones está en `OfertaService.java`, método `getOfertaConValidacion()`. Cada método de transición (tomar, escalar, aprobar, rechazar, devolver) valida que la oferta esté en el estado correcto antes de ejecutar la transición.

**Cómo agregar un estado nuevo:**
1. Agregar el estado al enum `EstadoOferta`
2. Agregar el caso de validación en `OfertaService`
3. Si es necesario, agregar endpoint en `OfertaController`

Ejemplo de agregar estado "CANCELADO":
```java
// En EstadoOferta.java
CANCELADO,

// En OfertaService.java - agregar validación
if (estadoRequerido == EstadoOferta.CANCELADO) {
    if (oferta.getEstado() != EstadoOferta.EN_PROCESO) {
        throw new BusinessException("Para cancelar, la oferta debe estar en EN_PROCESO");
    }
}
```

### 2. ¿Cómo se evita la concurrencia?

Se usa `@Version` en la entidad `Oferta` para bloqueo optimista. Si dos usuarios actúan sobre la misma oferta simultáneamente, el segundo recibe un error 409 (Conflict).

**Qué ve el usuario:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "El recurso fue modificado por otro usuario. Por favor, recargue y reintente."
}
```

### 3. ¿Cómo se maneja el JWT?

- **Duración:** 8 horas (28800 segundos)
- **Revocación:** No implementada en esta versión (simplificada)
- **Refresh tokens:** No implementados (re-login requerido)

### 4. ¿Decisiones para usuarios nuevos?

- Interfaz intuitiva con estados claros
- Mensajes de error descriptivos
- Validaciones en tiempo real en el frontend

**Mejora:** Agregar videos tutoriales en la página de login.

### 5. Ambigüedades del enunciado

| Ambigüedad | Suposición | Pregunta al equipo |
|------------|------------|-------------------|
| BRP con varias ofertas | Solo una oferta activa por BRP | ¿Qué pasa con ofertas rechazadas? ¿Se pueden crear nuevas? |
| Asignación de analista | Manual | ¿Hay distribución automática? |
| Adjuntos al rechazar | Se conservan | ¿Se deben borrar? |
| Historial de cambios | No especificado | ¿Se necesita auditoría de cambios de estado? |

**Mejora del enunciado:** Agregar diagrama UML del modelo y ejemplos JSON.

### 6. Pendientes

| Pendiente | Estado |
|-----------|--------|
| Frontend Angular | ✅ COMPLETADO (8 componentes) |
| Refresh tokens JWT | Simplificación para demo |
| Historial de ofertas | No exigido explícitamente |
| Tests unitarios | ✅ 29 tests, 29 pasan |

### 7. Inteligencia Artificial

**Agente utilizado:** Kiro (este asistente)

**Rol:**
- Generación de código (entidades, servicios, controladores)
- Resolución de bugs y debugging
- Documentación técnica
- Revisión de arquitectura

**Qué delegué:**
- Generación de código repetitivo (DTOs, repositorios)
- Configuración de seguridad JWT
- Migraciones Liquibase

**Qué NO delegué:**
- Diseño del modelo de datos
- Decisiones de arquitectura
- Interpretación de requisitos

### 8. Implementación en Django (hipotético)

**Nivel de familiaridad:** 2/5 (proyecto pequeño realizado)

**Planteamiento:**

```python
# models.py
class Oferta(models.Model):
    brp = models.ForeignKey(Brp, on_delete=models.CASCADE)
    estado = models.CharField(max_length=30, choices=ESTADO_CHOICES)
    radicado_por = models.ForeignKey(User, on_delete=models.CASCADE)
    analista = models.ForeignKey(User, null=True, related_name='analista_ofertas')
    created_at = models.DateTimeField(auto_now_add=True)
    version = models.PositiveIntegerField(default=0)  # Optimistic lock

# Django-FSM para transiciones
from django_fsm import FSMField, transition

class Oferta(models.Model):
    estado = FSMField(choices=ESTADO_CHOICES, default='RADICADO')
    
    @transition(field=estado, source='RADICADO', target='EN_PROCESO')
    def tomar(self):
        pass

# Seguridad con Django REST Framework
from rest_framework.permissions import IsAuthenticated

class OfertaViewSet(viewsets.ModelViewSet):
    permission_classes = [IsAuthenticated]
    
    def get_queryset(self):
        if self.request.user.rol == 'RADICADOR':
            return Oferta.objects.filter(radicado_por=self.request.user)
        return Oferta.objects.all()
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