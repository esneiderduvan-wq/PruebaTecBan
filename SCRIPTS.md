# Guía de Prueba del Flujo Completo

Este documento describe los pasos para probar el flujo completo del sistema de ofertas BRP.

## Preparación

### 1. Levantar la base de datos
```bash
docker-compose up -d
```

### 2. Levantar la aplicación
```bash
.\gradlew.bat :backend:bootRun
```

### 3. Verificar que está corriendo
```bash
curl http://localhost:8080/actuator/health
```

---

## Flujo de Prueba Completo

### Paso 1: Login como Radicador

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "radicador1", "password": "admin123"}'

# Guardar el token
TOKEN="eyJhbGciOiJIUzI1NiIs..."
```

### Paso 2: Consultar BRP disponibles

```bash
curl -X GET http://localhost:8080/api/brp/disponibles \
  -H "Authorization: Bearer $TOKEN"

# Copiar un código BRP disponible, ej: "BRP-0001"
```

### Paso 3: Radicar una oferta

```bash
curl -X POST http://localhost:8080/api/ofertas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "brpCodigo": "BRP-0001",
    "oferentes": [
      {
        "tipoIdentificacion": "CC",
        "numeroIdentificacion": "12345678",
        "nombre": "Juan Pérez",
        "porcentajeParticipacion": 100.00,
        "email": "juan@email.com",
        "telefono": "3001234567",
        "pagos": [
          {
            "tipoPago": "CONTADO",
            "fecha": "2026-11-20",
            "valor": 300000000
          }
        ]
      }
    ]
  }'

# Guardar el ID de la oferta, ej: 1
OFERTA_ID=1
```

### Paso 4: Verificar oferta radicaada

```bash
curl -X GET http://localhost:8080/api/ofertas/$OFERTA_ID \
  -H "Authorization: Bearer $TOKEN"

# Verificar que estado = "RADICADO"
```

### Paso 5: Logout y login como Analista

```bash
# Login como analista
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "analista1", "password": "admin123"}'

# Guardar nuevo token
TOKEN_ANALISTA="eyJhbGciOiJIUzI1NiIs..."
```

### Paso 6: Analista toma la oferta

```bash
curl -X POST http://localhost:8080/api/ofertas/$OFERTA_ID/tomar \
  -H "Authorization: Bearer $TOKEN_ANALISTA"

# Verificar que estado cambió a "EN_PROCESO" y tiene analista asignado
curl -X GET http://localhost:8080/api/ofertas/$OFERTA_ID \
  -H "Authorization: Bearer $TOKEN_ANALISTA"
```

### Paso 7: Escalar al Jefe

```bash
curl -X POST http://localhost:8080/api/ofertas/$OFERTA_ID/escalar \
  -H "Authorization: Bearer $TOKEN_ANALISTA"

# Verificar que estado cambió a "ESCALADO"
```

### Paso 8: Logout y login como Jefe

```bash
# Login como jefe
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "jefe1", "password": "admin123"}'

TOKEN_JEFE="eyJhbGciOiJIUzI1NiIs..."
```

### Paso 9: Jefe aprobar la oferta

```bash
curl -X GET http://localhost:8080/api/ofertas/escaladas \
  -H "Authorization: Bearer $TOKEN_JEFE"

# Verificar que aparece la oferta escalada

curl -X POST http://localhost:8080/api/ofertas/$OFERTA_ID/aprobar \
  -H "Authorization: Bearer $TOKEN_JEFE"

# Verificar que estado cambió a "APROBADO" y tiene fechaFinalizacion
```

---

## Pruebas de Escenarios de Error

### Escenario 1: Radicador intenta tomar su propia oferta

```bash
# Login como radicador
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "radicador1", "password": "admin123"}'

TOKEN_RAD="eyJ..."

# Crear nueva oferta
curl -X POST http://localhost:8080/api/ofertas \
  -H "Authorization: Bearer $TOKEN_RAD" \
  -H "Content-Type: application/json" \
  -d '{
    "brpCodigo": "BRP-0002",
    "oferentes": [{"tipoIdentificacion":"CC","numeroIdentificacion":"87654321","nombre":"María","porcentajeParticipacion":100.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":500000000}]}]
  }'

# Intentar tomar (debería fallar)
curl -X POST http://localhost:8080/api/ofertas/2/tomar \
  -H "Authorization: Bearer $TOKEN_RAD"

# Respuesta esperada: 409 Conflict - "No puede tomar una oferta que usted stesso radicó"
```

### Escenario 2: Oferta sobre BRP no DISPONIBLE

```bash
# Intentar radicar sobre BRP con otro estado
curl -X POST http://localhost:8080/api/ofertas \
  -H "Authorization: Bearer $TOKEN_RAD" \
  -H "Content-Type: application/json" \
  -d '{
    "brpCodigo": "BRP-0012",  # Este está en estado DADO_DE_BAJA
    "oferentes": [{"tipoIdentificacion":"CC","numeroIdentificacion":"111222333","nombre":"Pedro","porcentajeParticipacion":100.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":200000000}]}]
  }'

# Respuesta esperada: 409 Conflict - "Solo se puede ofertar sobre BRP en estado DISPONIBLE"
```

### Escenario 3: Más de 5 oferentes

```bash
curl -X POST http://localhost:8080/api/ofertas \
  -H "Authorization: Bearer $TOKEN_RAD" \
  -H "Content-Type: application/json" \
  -d '{
    "brpCodigo": "BRP-0003",
    "oferentes": [
      {"tipoIdentificacion":"CC","numeroIdentificacion":"1","nombre":"A","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"2","nombre":"B","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"3","nombre":"C","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"4","nombre":"D","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"5","nombre":"E","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"6","nombre":"F","porcentajeParticipacion":20.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":100000000}]}
    ]
  }'

# Respuesta esperada: 409 Conflict - "Máximo 5 oferentes por oferta"
```

### Escenario 4: Porcentajes no suman 100

```bash
curl -X POST http://localhost:8080/api/ofertas \
  -H "Authorization: Bearer $TOKEN_RAD" \
  -H "Content-Type: application/json" \
  -d '{
    "brpCodigo": "BRP-0004",
    "oferentes": [
      {"tipoIdentificacion":"CC","numeroIdentificacion":"1","nombre":"A","porcentajeParticipacion":50.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":50000000}]},
      {"tipoIdentificacion":"CC","numeroIdentificacion":"2","nombre":"B","porcentajeParticipacion":30.00,"pagos":[{"tipoPago":"CONTADO","fecha":"2026-11-20","valor":30000000}]}
    ]
  }'

# Respuesta esperada: 409 Conflict - "Los porcentajes de participación deben sumar 100%"
```

---

## Pruebas con Postman/Insomnia

### Colección Postman

```json
{
  "info": {
    "name": "BRP API Testing",
    "description": "Colección de pruebas para el sistema de ofertas BRP",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {"key": "baseUrl", "value": "http://localhost:8080"},
    {"key": "token", "value": ""}
  ],
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login Admin",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/auth/login",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {"mode": "raw", "raw": "{\"username\": \"admin\", \"password\": \"admin123\"}"}
          }
        }
      ]
    },
    {
      "name": "BRP",
      "item": [
        {
          "name": "Listar Disponibles",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/brp/disponibles",
            "header": [{"key": "Authorization", "value": "Bearer {{token}}"}]
          }
        }
      ]
    }
  ]
}
```

---

## Verificación de Códigos HTTP

| Escenario | Código Esperado |
|-----------|----------------|
| Login exitoso | 200 OK |
| Recurso creado | 201 Created |
| Recurso no encontrado | 404 Not Found |
| Error de validación | 400 Bad Request |
| Sin permisos | 403 Forbidden |
| Conflicto de negocio | 409 Conflict |
| Error de autenticación | 401 Unauthorized |

---

## Comandos Rápidos Resumidos

```bash
# 1. Levantar DB
docker-compose up -d

# 2. Arrancar app
.\gradlew.bat :backend:bootRun

# 3. Login y guardar token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' | jq -r '.token')

# 4. Listar BRP
curl http://localhost:8080/api/brp -H "Authorization: Bearer $TOKEN"

# 5. Listar ofertas
curl http://localhost:8080/api/ofertas -H "Authorization: Bearer $TOKEN"
```