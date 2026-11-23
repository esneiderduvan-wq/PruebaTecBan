# Respuestas a las Preguntas de la Prueba Técnica

## Pregunta 1: ¿Cómo armas el stack y el arranque?

### Stack Tecnológico Implementado

| Capa | Recomendación | Implementado | Descripción |
| :- | :- | :- | :- |
| JDK | Java 21 o Java 25 | OpenJDK 17 | Compatible con ambos |
| Backend | Spring Boot, JPA, Security, Bean Validation | ✅ Spring Boot 3.2.0 | Spring Data JPA + Spring Security 6 + Bean Validation |
| Frontend | Angular 17+ | ✅ Angular 17 | Standalone components |
| Base de datos | PostgreSQL Docker Compose | ✅ PostgreSQL 16 | Contenedor Docker |
| Migraciones | Liquibase versionado | ✅ 9 migraciones | Esquema + 100 BRP desde CSV |
| Adjuntos | Almacenamiento local en disco | ✅ `uploads/adjuntos/` | Archivos guardados físicamente |
| Seguridad | Login propio, BCrypt, JWT | ✅ BCrypt + JWT 8h | Usuarios en BD, no Active Directory |
| Build | Gradle 8+ | ✅ Gradle 8.5 | Gradle wrapper incluido |
| Pruebas | JUnit unitarias | ✅ 29 tests, 29 pasan | Cobertura de reglas principales |


## Pregunta 2: ¿Cómo modelas y cargas el catálogo de BRP?

### Modelo de Datos (Entidad Brp)

```java
@Entity
@Table(name = "brp")
public class Brp {
    @Id
    private String codigo;  // PK: BRP-0001, BRP-0002, etc.
    
    @Enumerated(EnumType.STRING)
    private EstadoBrp estado;  // DISPONIBLE, SIN_PRECIO, PENDIENTE_TECNICO, 
                               // EN_TRAMITE_DE_VENTA, SANEAMIENTO, DADO_DE_BAJA
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_brp")
    private TipoBrp tipoBrp;  // APARTAMENTO, CASA, LOCAL, LOTE, VEHICULO, MAQUINARIA
    
    @Column(name = "matricula_inmiaria")
    private String matriculaInmiaria;  // Inmuebles
    
    private String placa;  // Vehículos
    
    private String departamento;
    private String municipio;
    private String descripcion;
    
    @Column(precision = 15)
    private BigDecimal precio;
    
    @Column(name = "fecha_asignacion_precio")
    private LocalDateTime fechaAsignacionPrecio;
    
    private Integer anioIngesta;
    private Integer mesIngesta;
    private Integer diaIngesta;
}
```

### Reglas del Modelo Cumplidas

| Regla | Descripción | Implementación |
| :- | :- | :- |
| **Regla 1** | Si fecha_asignacion_precio es null → precio null y estado SIN_PRECIO | CHECK en migración + validación en `BrpService.aplicarReglasBrp()` |
| **Regla 2** | Matrícula y placa nunca van juntas | CHECK `matricula_inmiaria IS NULL OR placa IS NULL` |
| **Regla 3** | BRP único (última ingesta) | `codigo` es @Id (PK único), muestra el más reciente |

### Carga del Catálogo

```
brp_ejemplo.csv (100 BRPs: 25 apartamentos, 20 casas, 15 locales, 12 lotes, 18 vehículos, 10 maquinarias)
        ↓
generate-liquibase.ps1 (PowerShell)
        ↓
008-seed-brp-from-csv.xml (4 changesets, 25 registros c/u)
        ↓
master.xml (Liquibase orchestration)
        ↓
Al iniciar backend → Liquibase ejecuta automáticamente
```

### Script de Generación

```powershell
# Generar XML desde CSV
cd backend
.\generate-liquibase.ps1

# Resultado: src/main/resources/db/changelog/008-seed-brp-from-csv.xml
```

### Validación al Cargar

```java
public Brp aplicarReglasBrp(Brp brp) {
    // Regla 1: fecha y precio nacen juntos o ambos nulos
    if (brp.getFechaAsignacionPrecio() == null) {
        brp.setPrecio(null);
        brp.setEstado(EstadoBrp.SIN_PRECIO);
    }
    return brp;
}
```

### Endpoint de Información del Catálogo

```bash
GET /api/brp/catalogo/ultima-actualizacion
Authorization: Bearer <token>

Response:
{
  "ultimaActualizacion": "2026-11-23T12:00:00",
  "totalBrps": 100
}
```

---

## Pregunta 3: ¿Cómo modelas la oferta, los oferentes, los pagos y los adjuntos?

### Diagrama de Entidades

```
┌──────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│   Oferta │─────▶│ Oferente │──────▶│   Pago   │       │ Adjunto  │
│  (1..N)  │       │  (1..5)  │       │  (1..6)  │       │  (0..N)  │
└──────────┘       └──────────┘       └──────────┘       └──────────┘
     ↓                   ↓
┌──────────┐       ┌──────────┐
│   BRP    │       │ Usuario  │
│  (1..1)  │       │ (rad/ana)│
└──────────┘       └──────────┘
```

### Modelo Oferta

```java
@Entity
@Table(name = "oferta")
public class Oferta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brp_codigo", nullable = false)
    private Brp brp;
    
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;  // RADICADO, EN_PROCESO, ESCALADO, APROBADO, RECHAZADO
    
    @Column(precision = 15, nullable = false)
    private BigDecimal valorTotal;  // Suma de todos los pagos
    
    @Column(name = "descuento", precision = 5, scale = 2)
    private BigDecimal descuento;  // Calculado automáticamente
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "radicado_por_id", nullable = false)
    private Usuario radicadoPor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_id")
    private Usuario analista;
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Oferente> oferentes = new ArrayList<>();
    
    @OneToMany(mappedBy = "oferta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Adjunto> adjuntos = new ArrayList<>();
    
    // Campos de auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEscalamiento;
    private LocalDateTime fechaDevolucion;
    private LocalDateTime fechaFinalizacion;
    
    @Column(name = "motivo_devolucion")
    private String motivoDevolucion;
    
    @Version  // Control de concurrencia
    private Long version;
}
```

### Modelo Oferente (hasta 5 por oferta)

```java
@Entity
@Table(name = "oferente")
public class Oferente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;
    
    @Column(name = "tipo_identificacion", length = 20)
    private String tipoIdentificacion;  // CC, NIT
    
    @Column(name = "numero_identificacion", length = 20)
    private String numeroIdentificacion;
    
    @Column(length = 255)
    private String nombre;
    
    @Column(name = "porcentaje_participacion", precision = 5, scale = 2)
    private BigDecimal porcentajeParticipacion;  // Calculado automáticamente
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 20)
    private String telefono;
    
    @OneToMany(mappedBy = "oferente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pago> pagos = new ArrayList<>();
}
```

### Modelo Pago (hasta 6 por oferente)

```java
@Entity
@Table(name = "pago")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferente_id", nullable = false)
    private Oferente oferente;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", length = 20)
    private TipoPago tipoPago;  // CONTADO, PRESTAMO
    
    private LocalDate fecha;
    
    @Column(precision = 15)
    private BigDecimal valor;
    
    @Column(name = "entidad_financiadora", length = 100)
    private String entidadFinanciadora;  // Solo si es PRESTAMO
}
```

### Modelo Adjunto

```java
@Entity
@Table(name = "adjunto")
public class Adjunto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;
    
    @Column(name = "nombre_archivo", length = 255, nullable = false)
    private String nombreArchivo;
    
    @Column(name = "ruta_local", length = 500, nullable = false)
    private String rutaLocal;  // backend/uploads/adjuntos/1732300000000_archivo.pdf
    
    @Column(length = 255)
    private String descripcion;
    
    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();
}
```

### Reglas de Negocio Cumplidas

| Regla | Descripción | Implementación |
| :- | :- | :- |
| Máximo 5 oferentes | `if (request.getOferentes().size() > 5)` → BusinessException | ✅ |
| Máximo 6 pagos por oferente | `if (oferenteReq.getPagos().size() > 6)` → BusinessException | ✅ |
| Suma pagos = valorTotal | En `radicarOferta()`, suma todos los pagos | ✅ |
| Porcentaje participación automático | `(pagoOferente / total) * 100` | ✅ |
| Suma porcentajes = 100 | Implícito por cálculo de valor/pago | ✅ |
| Descuento automático | `(precioBRP - valorTotal) / precioBRP * 100` | ✅ |
| Transiciones válidas | `getOfertaConValidacion()` + @PreAuthorize | ✅ |
| Control de concurrencia | `@Version` en Oferta | ✅ |

### Cálculo Automático del Porcentaje

```java
@Transactional
public OfertaResponse radicarOferta(OfertaRequest request, String username) {
    Brp brp = brpRepository.findByCodigo(request.getBrpCodigo())
        .orElseThrow(() -> new ResourceNotFoundException("BRP no encontrado"));
    
    // Validar máximo 5 oferentes
    if (request.getOferentes().size() > 5) {
        throw new BusinessException("Máximo 5 oferentes por oferta");
    }
    
    Oferta oferta = Oferta.builder()
        .brp(brp)
        .estado(EstadoOferta.RADICADO)
        .radicadoPor(radicador)
        .fechaCreacion(LocalDateTime.now())
        .build();
    
    BigDecimal valorTotal = BigDecimal.ZERO;
    List<BigDecimal> valoresOferentes = new ArrayList<>();
    
    // 1. Calcular valor total y guardar valores por oferente
    for (OferenteRequest oferenteReq : request.getOferentes()) {
        BigDecimal pagosOferente = oferenteReq.getPagos().stream()
            .map(PagoRequest::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        valoresOferentes.add(pagosOferente);
        valorTotal = valorTotal.add(pagosOferente);
    }
    
    // 2. Validar valor total > 0
    if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("El valor total de la oferta debe ser mayor a 0");
    }
    
    // 3. Crear oferentes con porcentaje calculado
    int i = 0;
    for (OferenteRequest oferenteReq : request.getOferentes()) {
        BigDecimal valorOferente = valoresOferentes.get(i);
        BigDecimal porcentaje = valorOferente.multiply(new BigDecimal("100"))
            .divide(valorTotal, 2, RoundingMode.HALF_UP);
        
        Oferente oferente = Oferente.builder()
            .oferta(oferta)
            .tipoIdentificacion(oferenteReq.getTipoIdentificacion())
            .numeroIdentificacion(oferenteReq.getNumeroIdentificacion())
            .nombre(oferenteReq.getNombre())
            .porcentajeParticipacion(porcentaje)  // Calculado automáticamente
            .email(oferenteReq.getEmail())
            .telefono(oferenteReq.getTelefono())
            .build();
        
        // Agregar pagos...
        oferta.getOferentes().add(oferente);
        i++;
    }
    
    // 4. Calcular descuento
    BigDecimal descuento = calcularDescuento(brp.getPrecio(), valorTotal);
    oferta.setValorTotal(valorTotal);
    oferta.setDescuento(descuento);
    
    return toResponse(ofertaRepository.save(oferta));
}
```

### Cálculo del Descuento

```java
private BigDecimal calcularDescuento(BigDecimal precio, BigDecimal valorOferta) {
    if (precio == null || precio.compareTo(BigDecimal.ZERO) == 0) {
        return null;
    }
    return precio.subtract(valorOferta)
        .divide(precio, 4, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
    // Negativo = sobreprecio (ej: -10% = 10% por encima del precio)
}
```

### Carga de Adjuntos

```bash
# Subir documento a una oferta
POST /api/adjuntos/upload/{ofertaId}
Content-Type: multipart/form-data

Parameters:
- file: archivo.pdf
- nombre: "Documento de la oferta"

# Ver adjuntos de una oferta
GET /api/adjuntos/oferta/{ofertaId}

# Descargar adjunto
GET /api/adjuntos/download/{id}
```

### Resumen de Funcionalidades

| Funcionalidad | Estado |
| :- | :- |
| Modelo Oferta con BRP, valorTotal, descuento, estados | ✅ |
| Modelo Oferente con % participación automático | ✅ |
| Modelo Pago (CONTADO/PRESTAMO) hasta 6 por oferente | ✅ |
| Modelo Adjunto con almacenamiento en disco | ✅ |
| Validación máximo 5 oferentes | ✅ |
| Validación máximo 6 pagos por oferente | ✅ |
| Cálculo automático de porcentaje | ✅ |
| Cálculo automático de descuento | ✅ |
| Control de concurrencia con @Version | ✅ |

---

## Pregunta 4: ¿Cómo implementas el flujo de estados de la oferta?

### Diagrama de Estados

```
                          ┌─────────────────┐
                          │                 │
┌──────────────►  RADICADO│                 │
│      ┌────────────────►│                  │
│      │                 │                  │
│      │                 ▼                  │
│      │         ┌─────────────┐            │
│      │         │             │            │
│      │         │ EN_PROCESO  │            │
│      │         │             │            │
│      │         └──────┬──────┘            │
│      │                │                   │
│      │     ┌──────────┴──────────┐        │
│      │     │                     │        │
│      │     ▼                     ▼        │
│      │ ┌─────────┐         ┌─────────┐    │
│      │ │ ESCALADO│         │ RECHAZADO│◄──┘
│      │ │         │         │         │
│      │ └────┬────┘         └─────────┘
│      │      │
│      │      │   ┌─────────────────────┐
│      │      │   │                     │
│      │      └──►│   APROBADO (FIN)    │
│      │          │                     │
│      │          └─────────────────────┘
│      │
│      │   ┌─────────────────────────────────────┐
│      │   │  EN_PROCESO (devuelto) ◄────────────┘
│      │   │      │
│      │   ▼      ▼
│      │ ┌─────────────────────┐
│      │ │ RECHAZADO (jefe)    │◄──────────┐
│      │ └──────────▲──────────┘           │
│      │           │                       │
│      │           └───────────────────────┘
│      │
│      └─────────────────────────────────────────────────────────► RECHAZADO (analista)
```

### Estados de Oferta

```java
public enum EstadoOferta {
    RADICADO,     // Oferta nueva, esperando分析师
    EN_PROCESO,   // Analyst tomó la oferta
    ESCALADO,     // Analyst la envió a aprobación
    APROBADO,     // Jefe aprobó (estado final)
    RECHAZADO     // Rechazada (estado final)
}
```

### Transiciones por Rol

| Rol | Transiciones permitidas |
| :- | :- |
| ANALISTA | RADICADO → EN_PROCESO, EN_PROCESO → ESCALADO, EN_PROCESO → RECHAZADO |
| JEFE | ESCALADO → APROBADO, ESCALADO → EN_PROCESO (devolver), ESCALADO → RECHAZADO |

### Implementación en OfertaController

```java
@RestController
@RequestMapping("/api/ofertas")
@RequiredArgsConstructor
public class OfertaController {

    private final OfertaService ofertaService;

    @PostMapping("/{id}/tomar")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<OfertaResponse> tomar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.tomarOferta(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/escalar")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<OfertaResponse> escalar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.escalarOferta(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('JEFE')")
    public ResponseEntity<OfertaResponse> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.aprobarOferta(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ANALISTA', 'JEFE')")
    public ResponseEntity<OfertaResponse> rechazar(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String motivo,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.rechazarOferta(id, userDetails.getUsername(), motivo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('JEFE')")
    public ResponseEntity<OfertaResponse> devolver(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String motivo,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.devolverOferta(id, userDetails.getUsername(), motivo);
        return ResponseEntity.ok(response);
    }
}
```

### Validación de Transiciones (OfertaService)

```java
@Transactional
public OfertaResponse tomarOferta(Long id, String username) {
    Oferta oferta = ofertaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));

    // Validación: solo RADICADO puede pasar a EN_PROCESO
    if (oferta.getEstado() != EstadoOferta.RADICADO) {
        throw new BusinessException("Solo se pueden tomar ofertas en estado RADICADO");
    }

    // Validación: analista no puede tomar su propia oferta
    if (oferta.getRadicadoPor().getId().equals(analista.getId())) {
        throw new BusinessException("No puede tomar una oferta que usted stesso radicó");
    }

    oferta.setEstado(EstadoOferta.EN_PROCESO);
    oferta.setAnalista(analista);
    oferta.setFechaModificacion(LocalDateTime.now());

    return toResponse(ofertaRepository.save(oferta));
}

@Transactional
public OfertaResponse rechazarOferta(Long id, String username, String motivo) {
    Oferta oferta = ofertaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));

    boolean esAnalista = SecurityContextHolder.getContext().getAuthentication()
        .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANALISTA"));
    boolean esJefe = SecurityContextHolder.getContext().getAuthentication()
        .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JEFE"));

    // Validar transiciones
    if (esAnalista && oferta.getEstado() == EstadoOferta.EN_PROCESO) {
        // OK - Analista rechazando
    } else if (esJefe && oferta.getEstado() == EstadoOferta.ESCALADO) {
        // OK - Jefe rechazando
    } else {
        throw new BusinessException("No tiene permisos para rechazar esta oferta en su estado actual");
    }

    // Al rechazar, el BRP vuelve a DISPONIBLE
    oferta.getBrp().setEstado(EstadoBrp.DISPONIBLE);

    oferta.setEstado(EstadoOferta.RECHAZADO);
    oferta.setFechaFinalizacion(LocalDateTime.now());
    oferta.setFechaModificacion(LocalDateTime.now());

    return toResponse(ofertaRepository.save(oferta));
}

@Transactional
public OfertaResponse aprobarOferta(Long id, String username) {
    Oferta oferta = getOfertaConValidacion(id, username, EstadoOferta.ESCALADO);

    oferta.setEstado(EstadoOferta.APROBADO);
    oferta.setFechaFinalizacion(LocalDateTime.now());
    oferta.setFechaModificacion(LocalDateTime.now());

    return toResponse(ofertaRepository.save(oferta));
}

@Transactional
public OfertaResponse devolverOferta(Long id, String username, String motivo) {
    Oferta oferta = getOfertaConValidacion(id, username, EstadoOferta.ESCALADO);

    // Validar que no se devuelva al mismo analista
    if (oferta.getAnalista().getUsername().equals(username)) {
        throw new BusinessException("No puede devolver la oferta al mismo analista");
    }

    oferta.setEstado(EstadoOferta.EN_PROCESO);
    oferta.setFechaDevolucion(LocalDateTime.now());
    oferta.setMotivoDevolucion(motivo);
    oferta.setFechaModificacion(LocalDateTime.now());

    return toResponse(ofertaRepository.save(oferta));
}

private Oferta getOfertaConValidacion(Long id, String username, EstadoOferta estadoRequerido) {
    Oferta oferta = ofertaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));

    if (estadoRequerido != null && oferta.getEstado() != estadoRequerido) {
        throw new BusinessException("La oferta debe estar en estado " + estadoRequerido + " para esta operación");
    }

    return oferta;
}
```

### Transiciones Resumidas

| Desde | Hacia | Quién | Endpoint | Acción adicional |
| :- | :- | :- | :- | :- |
| RADICADO | EN_PROCESO | ANALISTA | POST /{id}/tomar | Asigna analista |
| EN_PROCESO | ESCALADO | ANALISTA | POST /{id}/escalar | - |
| EN_PROCESO | RECHAZADO | ANALISTA | POST /{id}/rechazar | BRP → DISPONIBLE |
| ESCALADO | APROBADO | JEFE | POST /{id}/aprobar | fechaFinalizacion |
| ESCALADO | EN_PROCESO | JEFE | POST /{id}/devolver | fechaDevolucion, motivo |
| ESCALADO | RECHAZADO | JEFE | POST /{id}/rechazar | BRP → DISPONIBLE |

### Códigos de Respuesta HTTP

| Escenario | Código | Mensaje |
| :- | :- | :- |
| Transición válida | 200 OK | Oferta actualizada |
| Oferta no encontrada | 404 Not Found | "Oferta no encontrada: X" |
| Estado inválido | 409 Conflict | "La oferta debe estar en estado X para esta operación" |
| Sin permisos | 403 Forbidden | @PreAuthorize bloquea |
| Error de negocio | 409 Conflict | Mensaje descriptivo |

---

## Pregunta 5: ¿Cómo implementas la seguridad, administración de módulos y auditoría?

### Arquitectura de Seguridad

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Security 6                        │
├─────────────────────────────────────────────────────────────┤
│  • JWT Authentication Filter                                │
│  • BCrypt Password Encoding (strength = 10)                 │
│  • Method Security (@PreAuthorize)                          │
│  • CORS configurado para Angular                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      JWT Token                              │
├─────────────────────────────────────────────────────────────┤
│  • issuer: Bancolombia BRP                                  │
│  • expiration: 8 horas (28800 segundos)                     │
│  • Claims: sub (username), roles, rol                       │
└─────────────────────────────────────────────────────────────┘
```

### Modelo de Usuario con Roles y Módulos

```java
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;  // BCrypt encoded
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;  // ADMINISTRADOR, RADICADOR, ANALISTA, JEFE
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "usuario_modulos",
        joinColumns = @JoinColumn(name = "usuario_id")
    )
    @Column(name = "modulo")
    private Set<String> modulos = new HashSet<>();
    
    private Boolean activo = true;
    
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
}
```

### Módulos por Rol

```java
// Constants.java
public static final Set<String> MODULOS_ADMIN = Set.of(
    "CATALOGO", "SEGUIMIENTO", "ADMINISTRACION"
);

public static final Set<String> MODULOS_RADICADOR = Set.of(
    "CATALOGO", "RADICACION", "SEGUIMIENTO"
);

public static final Set<String> MODULOS_ANALISTA = Set.of(
    "ANALISIS", "SEGUIMIENTO"
);

public static final Set<String> MODULOS_JEFE = Set.of(
    "SEGUIMIENTO", "APROBACION"
);
```

### Asignación Automática de Módulos

```java
@Transactional
public Usuario crearUsuario(UsuarioRequest request) {
    // Validar username único
    if (usuarioRepository.existsByUsername(request.getUsername())) {
        throw new BusinessException("El username ya existe");
    }
    
    // Asignar módulos según el rol
    Set<String> modulos = new HashSet<>();
    switch (request.getRol()) {
        case ADMINISTRADOR:
            modulos.addAll(MODULOS_ADMIN);
            break;
        case RADICADOR:
            modulos.addAll(MODULOS_RADICADOR);
            break;
        case ANALISTA:
            modulos.addAll(MODULOS_ANALISTA);
            break;
        case JEFE:
            modulos.addAll(MODULOS_JEFE);
            break;
    }
    
    Usuario usuario = Usuario.builder()
        .username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))
        .email(request.getEmail())
        .rol(request.getRol())
        .modulos(modulos)
        .activo(true)
        .fechaCreacion(LocalDateTime.now())
        .build();
    
    return usuarioRepository.save(usuario);
}
```

### Protección de Endpoints

```java
@RestController
@RequestMapping("/api/ofertas")
@RequiredArgsConstructor
public class OfertaController {

    @PostMapping
    @PreAuthorize("hasAnyRole('RADICADOR', 'ADMINISTRADOR')")
    public ResponseEntity<OfertaResponse> crear(@RequestBody @Valid OfertaRequest request) {
        // ...
    }

    @PostMapping("/{id}/tomar")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<OfertaResponse> tomar(@PathVariable Long id) {
        // ...
    }

    @PostMapping("/{id}/escalar")
    @PreAuthorize("hasRole('ANALISTA')")
    public ResponseEntity<OfertaResponse> escalar(@PathVariable Long id) {
        // ...
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('JEFE')")
    public ResponseEntity<OfertaResponse> aprobar(@PathVariable Long id) {
        // ...
    }
}
```

### Filtro de BRP por Rol

```java
@GetMapping("/disponibles")
@PreAuthorize("hasAnyRole('RADICADOR', 'ADMINISTRADOR')")
public ResponseEntity<List<BrpResponse>> getBrpDisponibles(
        @RequestHeader(value = "X-Rol", required = false) String rolHeader) {
    
    String rol = rolHeader != null ? rolHeader : 
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .findFirst()
            .map(a -> a.replace("ROLE_", ""))
            .orElse(null);
    
    List<Brp> brps;
    if ("RADICADOR".equals(rol)) {
        // Radicador solo ve BRP DISPONIBLE con precio asignado
        brps = brpRepository.findByEstadoAndPrecioIsNotNull(EstadoBrp.DISPONIBLE);
    } else {
        // Administrador ve todos
        brps = brpRepository.findAll();
    }
    
    return ResponseEntity.ok(brps.stream().map(this::toResponse).toList());
}
```

### Auditoría con TimestampedEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class TimestampedEntity {
    
    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
    
    @LastModifiedDate
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
    
    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private String creadoPor;
    
    @LastModifiedBy
    @Column(name = "modificado_por")
    private String modificadoPor;
}
```

### Resumen de Seguridad

| Aspecto | Implementación |
| :- | :- |
| Autenticación | JWT conBCrypt |
| Autorización | @PreAuthorize por rol |
| Roles | ADMINISTRADOR, RADICADOR, ANALISTA, JEFE |
| Módulos | Asignación automática por rol |
| Auditoría | @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy |
| Contraseñas | BCrypt strength 10 |
| Token | Expira en 8 horas |
| CORS | Configurado para localhost:4200 |

---

## Pregunta 6: ¿Cómo implementas las validaciones de reglas de negocio?

### Validaciones a Nivel de Entidad

```java
@Entity
@Table(name = "brp")
public class Brp {
    
    // CHECK constraint en base de datos
    @PrePersist
    @PreUpdate
    public void validar() {
        // Regla 1: Matrícula y placa nunca van juntas
        if (matriculaInmiaria != null && placa != null) {
            throw new BusinessException(
                "Un BRP no puede tener simultáneamente matrícula inmobiliaria y placa de vehículo");
        }
    }
}
```

### Constraints de Base de Datos (Liquibase)

```xml
<changeSet id="006-add-brp-checks" author="bancolombia">
    <!-- Matrícula y placa nunca van juntas -->
    <addCheckConstraint 
        tableName="brp" 
        constraintName="chk_brp_matricula_placa"
        expression="matricula_inmiaria IS NULL OR placa IS NULL"/>
    
    <!-- Estado coherente con precio -->
    <addCheckConstraint 
        tableName="brp" 
        constraintName="chk_brp_estado_precio"
        expression="(estado != 'SIN_PRECIO' AND precio IS NOT NULL) OR 
                   (estado = 'SIN_PRECIO' AND precio IS NULL)"/>
    
    <!-- Precio positivo -->
    <addCheckConstraint 
        tableName="brp" 
        constraintName="chk_brp_precio_positivo"
        expression="precio IS NULL OR precio &gt; 0"/>
</changeSet>
```

### Validaciones en Servicio (Oferta)

```java
@Transactional
public OfertaResponse radicarOferta(OfertaRequest request, String username) {
    // ========== VALIDACIONES DE NEGOCIO ==========
    
    // 1. BRP debe existir y estar DISPONIBLE
    Brp brp = brpRepository.findByCodigo(request.getBrpCodigo())
        .orElseThrow(() -> new ResourceNotFoundException("BRP no encontrado: " + request.getBrpCodigo()));
    
    if (brp.getEstado() != EstadoBrp.DISPONIBLE) {
        throw new BusinessException(
            "El BRP " + brp.getCodigo() + " no está disponible. Estado actual: " + brp.getEstado());
    }
    
    // 2. BRP debe tener precio asignado
    if (brp.getPrecio() == null) {
        throw new BusinessException(
            "El BRP " + brp.getCodigo() + " no tiene precio asignado");
    }
    
    // 3. Máximo 5 oferentes
    if (request.getOferentes() == null || request.getOferentes().isEmpty()) {
        throw new BusinessException("La oferta debe tener al menos un oferente");
    }
    if (request.getOferentes().size() > 5) {
        throw new BusinessException("Máximo 5 oferentes por oferta. Tiene: " + request.getOferentes().size());
    }
    
    // 4. Máximo 6 pagos por oferente
    for (OferenteRequest oferenteReq : request.getOferentes()) {
        if (oferenteReq.getPagos().size() > 6) {
            throw new BusinessException(
                "Máximo 6 pagos por oferente. Oferente: " + oferenteReq.getNombre());
        }
    }
    
    // 5. Valor total debe ser mayor a 0
    BigDecimal valorTotal = calcularValorTotal(request);
    if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException("El valor total de la oferta debe ser mayor a 0");
    }
    
    // 6. Valor total no puede ser mayor al precio del BRP + 10%
    BigDecimal precioMaximo = brp.getPrecio().multiply(new BigDecimal("1.10"));
    if (valorTotal.compareTo(precioMaximo) > 0) {
        throw new BusinessException(
            "El valor total de la oferta no puede exceder el 110% del precio del BRP. " +
            "Precio BRP: $" + brp.getPrecio() + ", Valor oferta: $" + valorTotal);
    }
    
    // ========== LÓGICA DE NEGOCIO ==========
    
    // Calcular porcentajes automáticamente
    BigDecimal[] resultado = calcularPorcentajes(request, valorTotal);
    
    // Calcular descuento
    BigDecimal descuento = calcularDescuento(brp.getPrecio(), valorTotal);
    
    // Guardar oferta...
}
```

### Validaciones con Bean Validation

```java
public class OfertaRequest {
    
    @NotBlank(message = "El código BRP es obligatorio")
    private String brpCodigo;
    
    @NotEmpty(message = "Debe incluir al menos un oferente")
    @Size(max = 5, message = "Máximo 5 oferentes")
    private List<OferenteRequest> oferentes;
    
    // Getters y Setters
}

public class OferenteRequest {
    
    @NotBlank(message = "El tipo de identificación es obligatorio")
    private String tipoIdentificacion;
    
    @NotBlank(message = "El número de identificación es obligatorio")
    private String numeroIdentificacion;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotEmpty(message = "Debe incluir al menos un pago")
    @Size(max = 6, message = "Máximo 6 pagos por oferente")
    private List<PagoRequest> pagos;
    
    @Email(message = "Debe ser un email válido")
    private String email;
    
    // Getters y Setters
}
```

### Manejo de Excepciones

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Error de negocio",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Recurso no encontrado",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Error de validación",
            errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Resumen de Validaciones

| Regla | Tipo | Lugar |
| :- | :- | :- |
| BRP disponible | Check | BD + Servicio |
| Precio asignado | Check | BD + Servicio |
| Máximo 5 oferentes | Bean + Servicio | Request + Servicio |
| Máximo 6 pagos | Bean + Servicio | Request + Servicio |
| Matrícula/placa exclusivos | Check | BD + Entity |
| Valor total > 0 | Servicio | Servicio |
| Valor total ≤ 110% precio | Servicio | Servicio |
| Porcentajes automáticos | Servicio | Servicio |
| Transiciones válidas | Servicio | Servicio |

---

## Pregunta 7: ¿Cómo implementas el seguimiento de ofertas y la frescura del catálogo?

### Dashboard de Seguimiento por Rol

```typescript
// seguimiento.component.ts
ngOnInit() {
  this.ofertaService.getMisOfertas().subscribe(ofertas => {
    this.ofertas = ofertas;
    
    // Contadores para el dashboard
    this.stats = {
      total: ofertas.length,
      radicados: ofertas.filter(o => o.estado === 'RADICADO').length,
      enProceso: ofertas.filter(o => o.estado === 'EN_PROCESO').length,
      escalados: ofertas.filter(o => o.estado === 'ESCALADO').length,
      aprobados: ofertas.filter(o => o.estado === 'APROBADO').length,
      rechazados: ofertas.filter(o => o.estado === 'RECHAZADO').length,
      conAdjuntos: ofertas.filter(o => o.tieneAdjuntos).length
    };
  });
}
```

### Indicadores de Frescura

```typescript
// Dashboard con indicadores de tiempo
interface OfertaDashboard {
  id: number;
  brpCodigo: string;
  estado: string;
  diasEnEstado: number;
  tieneAdjuntos: boolean;
  totalAdjuntos: number;
  descuento: number;
  valorTotal: number;
  fechaCreacion: string;
}

// Alertas visuales para ofertas "antiguas"
getAlertaClase(oferta: OfertaDashboard): string {
  if (oferta.diasEnEstado > 7) return 'text-danger';  // Más de 7 días
  if (oferta.diasEnEstado > 3) return 'text-warning'; // Más de 3 días
  return 'text-success';
}
```

### Endpoint de Estado del Catálogo

```java
@GetMapping("/catalogo/ultima-actualizacion")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RADICADOR')")
public ResponseEntity<CatalogoStatusResponse> getCatalogoStatus() {
    catalogoStatus = CatalogoStatusResponse.builder()
        .ultimaActualizacion(LocalDateTime.now())
        .totalBrps(brpRepository.count())
        .disponibles(brpRepository.countByEstado(EstadoBrp.DISPONIBLE))
        .sinPrecio(brpRepository.countByEstado(EstadoBrp.SIN_PRECIO))
        .enTramite(brpRepository.countByEstado(EstadoBrp.EN_TRAMITE_DE_VENTA))
        .ultimaIngesta(brpRepository.findTopByOrderByFechaModificacionDesc()
            .map(Brp::getFechaModificacion)
            .orElse(null))
        .build();
    
    return ResponseEntity.ok(catalogoStatus);
}
```

### Reporte de Frescura del Catálogo

```java
@GetMapping("/catalogo/reporte-frescura")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<List<BrpFrescuraResponse>> getReporteFrescura() {
    List<Brp> brps = brpRepository.findAll();
    
    LocalDateTime ahora = LocalDateTime.now();
    
    List<BrpFrescuraResponse> reporte = brps.stream().map(brp -> {
        long diasSinModificar = ChronoUnit.DAYS.between(
            brp.getFechaModificacion(), ahora);
        
        return BrpFrescuraResponse.builder()
            .codigo(brp.getCodigo())
            .estado(brp.getEstado())
            .diasSinModificar(diasSinModificar)
            .precio(brp.getPrecio())
            .ultimaModificacion(brp.getFechaModificacion())
            .alerta(diasSinModificar > 30 ? "VIEJO" : diasSinModificar > 7 ? "MEDIO" : "FRESCO")
            .build();
    }).toList();
    
    return ResponseEntity.ok(reporte);
}
```

### Historial de Cambios de Estado

```java
// Endpoint para ver el historial completo de una oferta
@GetMapping("/{id}/historial")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ANALISTA', 'JEFE')")
public ResponseEntity<List<HistorialResponse>> getHistorial(@PathVariable Long id) {
    Oferta oferta = ofertaRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada"));
    
    List<HistorialResponse> historial = new ArrayList<>();
    
    // Agregar cada transición
    historial.add(new HistorialResponse("Creación", 
        oferta.getRadicadoPor().getUsername(), 
        null, "RADICADO",
        oferta.getFechaCreacion()));
    
    if (oferta.getFechaEscalamiento() != null) {
        historial.add(new HistorialResponse("Escalamiento", 
            oferta.getAnalista().getUsername(),
            "EN_PROCESO", "ESCALADO",
            oferta.getFechaEscalamiento()));
    }
    
    if (oferta.getFechaDevolucion() != null) {
        historial.add(new HistorialResponse("Devolución", 
            "JEFE",
            "ESCALADO", "EN_PROCESO",
            oferta.getFechaDevolucion(),
            oferta.getMotivoDevolucion()));
    }
    
    if (oferta.getFechaFinalizacion() != null) {
        historial.add(new HistorialResponse("Finalización", 
            oferta.getAnalista().getUsername() != null ? 
                oferta.getAnalista().getUsername() : "JEFE",
            oferta.getEstado() == EstadoOferta.APROBADO ? "ESCALADO" : "EN_PROCESO",
            oferta.getEstado().name(),
            oferta.getFechaFinalizacion()));
    }
    
    return ResponseEntity.ok(historial);
}
```

### Resumen de Seguimiento

| Funcionalidad | Descripción |
| :- | :- |
| Dashboard por rol | Contadores de ofertas por estado |
| Historial | Trazabilidad completa de transiciones |
| Frescura BRP | Días desde última modificación |
| Alertas | Ofertas >7 días en estado |
| Reporte Admin | Catálogo completo con indicadores |
| Adjuntos | Indicadores de documentos adjuntos |

---

## Pregunta 8: ¿Cómo implementas la página de inicio según el rol?

### Router con Guards

```typescript
// app.routes.ts
export const routes: Routes = [
  { 
    path: '', 
    component: HomeComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'catalogo',
    component: CatalogoComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMINISTRADOR', 'RADICADOR'] }
  },
  {
    path: 'radicacion',
    component: RadicarOfertaComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMINISTRADOR', 'RADICADOR'] }
  },
  {
    path: 'analisis',
    component: AnalisisComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ANALISTA'] }
  },
  {
    path: 'aprobacion',
    component: AprobacionComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['JEFE'] }
  },
  {
    path: 'seguimiento',
    component: SeguimientoComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'administracion',
    component: AdministracionComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMINISTRADOR'] }
  }
];
```

### Menú Dinámico

```typescript
// app.component.ts
interface MenuItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

getMenuItems(): MenuItem[] {
  const rol = this.authService.getUserRole();
  
  const items: MenuItem[] = [
    { label: 'Inicio', icon: 'home', route: '/', roles: ['ADMINISTRADOR', 'RADICADOR', 'ANALISTA', 'JEFE'] },
    { label: 'Catálogo', icon: 'list', route: '/catalogo', roles: ['ADMINISTRADOR', 'RADICADOR'] },
    { label: 'Radicar Oferta', icon: 'add', route: '/radicacion', roles: ['ADMINISTRADOR', 'RADICADOR'] },
    { label: 'Análisis', icon: 'search', route: '/analisis', roles: ['ANALISTA'] },
    { label: 'Aprobación', icon: 'check', route: '/aprobacion', roles: ['JEFE'] },
    { label: 'Seguimiento', icon: 'visibility', route: '/seguimiento', roles: ['ADMINISTRADOR', 'RADICADOR', 'ANALISTA', 'JEFE'] },
    { label: 'Administración', icon: 'settings', route: '/administracion', roles: ['ADMINISTRADOR'] }
  ];
  
  return items.filter(item => item.roles.includes(rol));
}
```

### Pantalla de Inicio por Rol

```typescript
// home.component.ts
interface DashboardData {
  totalOfertas: number;
  pendientes: number;
  escaladas: number;
  aprobados: number;
  rechazados: number;
}

ngOnInit() {
  this.loadDashboard();
}

loadDashboard() {
  const rol = this.authService.getUserRole();
  
  switch (rol) {
    case 'ADMINISTRADOR':
      this.loadAdminDashboard();
      break;
    case 'RADICADOR':
      this.loadRadicadorDashboard();
      break;
    case 'ANALISTA':
      this.loadAnalistaDashboard();
      break;
    case 'JEFE':
      this.loadJefeDashboard();
      break;
  }
}

loadAdminDashboard() {
  // Ve todo: resumen general del sistema
  this.dashboardData = {
    totalOfertas: 0,
    pendientes: 0,  // RADICADO + EN_PROCESO
    escaladas: 0,    // ESCALADO
    aprobados: 0,
    rechazados: 0
  };
  this.ofertaService.getTodasLasOfertas().subscribe(datos => {
    this.stats = datos;
  });
}

loadRadicadorDashboard() {
  // Solo ve sus ofertas radicadas
  this.ofertaService.getMisOfertas().subscribe(ofertas => {
    this.stats = {
      total: ofertas.length,
      radicados: ofertas.filter(o => o.estado === 'RADICADO').length,
      enProceso: ofertas.filter(o => o.estado === 'EN_PROCESO').length,
      escalados: ofertas.filter(o => o.estado === 'ESCALADO').length,
      ...
    };
  });
}

loadAnalistaDashboard() {
  // Ve ofertas tomadas y las puede escalar
  this.ofertaService.getOfertasAsignadas().subscribe(ofertas => {
    this.ofertas = ofertas;
  });
}

loadJefeDashboard() {
  // Ve solo las escaladas para aprobar
  this.ofertaService.getOfertasEscaladas().subscribe(ofertas => {
    this.ofertas = ofertas;
  });
}
```

### Vista de Inicio por Rol

| Rol | Contenido del Dashboard |
| :- | :- |
| **ADMINISTRADOR** | Resumen general de todas las ofertas, contadores por estado, acceso a todos los módulos |
| **RADICADOR** | Sus ofertas radicadas, botón rápido para radicar nueva, список con estado de sus ofertas |
| **ANALISTA** | Ofertas tomadas para análisis, botón para escalar, botón para rechazar |
| **JEFE** | Ofertas escaladas esperando aprobación, botones para aprobar/devolver/rechazar |

### Botones de Acción Rápida

```typescript
// Tarjetas de acción rápida según rol
getQuickActions(): QuickAction[] {
  const rol = this.authService.getUserRole();
  
  switch (rol) {
    case 'RADICADOR':
      return [
        { label: 'Nueva Oferta', icon: 'add', action: () => this.router.navigate(['/radicacion']) },
        { label: 'Ver Mis Ofertas', icon: 'list', action: () => this.router.navigate(['/seguimiento']) }
      ];
    case 'ANALISTA':
      return [
        { label: 'Ver Mis Ofertas', icon: 'work', action: () => this.router.navigate(['/analisis']) }
      ];
    case 'JEFE':
      return [
        { label: 'Ofertas por Aprobar', icon: 'check', action: () => this.router.navigate(['/aprobacion']) }
      ];
    default:
      return [];
  }
}
```

---

## Respuestas a las Preguntas de la Sección 9

### 1. ¿Dónde vive la regla de transiciones de estado y cómo agregarías un estado nuevo sin tocar varios archivos?

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

### 2. ¿Cómo evitaste que dos personas actúen a la vez sobre la misma oferta, y qué ve el usuario cuando chocan?

Se usa `@Version` en la entidad `Oferta` para bloqueo optimista. Si dos usuarios actúan sobre la misma oferta simultáneamente, el segundo recibe un error 409 (Conflict).

**Qué ve el usuario:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "El recurso fue modificado por otro usuario. Por favor, recargue y reintente."
}
```

### 3. ¿Cómo manejaste el JWT: duración, revocación y refresh tokens?

- **Duración:** 8 horas (28800 segundos)
- **Revocación:** No implementada en esta versión
- **Refresh tokens:** No implementados re-login requerido, dado que esta a 8 horas

### 4. ¿Qué decisiones tomaste pensando en usuarios que ven el sistema por primera vez, y qué mejorarías de tu solución?

- Interfaz intuitiva con estados claros
- Mensajes de error descriptivos
- Validaciones en tiempo real en el frontend

**Mejora:** Agregar videos tutoriales en la página de login.

### 5. ¿Qué encontraste ambiguo, contradictorio o incompleto en este enunciado, qué supusiste en cada caso y qué le preguntarías al equipo de negocio antes de construir? ¿Y qué mejorarías del enunciado?

| Ambigüedad | Suposición | Pregunta al equipo |
|------------|------------|-------------------|
| BRP con varias ofertas | Solo una oferta activa por BRP | ¿Qué pasa con ofertas rechazadas? ¿Se pueden crear nuevas? |
| Asignación de analista | Manual | ¿Hay distribución automática? |
| Adjuntos al rechazar | Se conservan | ¿Se deben borrar? |
| Historial de cambios | No especificado | ¿Se necesita auditoría de cambios de estado? |

### Ambigüedades Resueltas

| Ambigüedad | Interpretación | Decisión |
| :- | :- | :- |
| **% participación oferentes** | ¿Se ingresa manual o automático? | Automático: `(valor_pago / valor_total) * 100` |
| **Estado después de rechazar** | ¿El BRP qué estado tiene? | Vuelve a DISPONIBLE |
| **¿Módulos manuales o automáticos?** | No especificado | Automáticos según rol (más simple y seguro) |


**Mejora del enunciado:** Agregar diagrama UML del modelo y ejemplos JSON. aparte de responder la ambigüedades

### 6. ¿Qué quedó pendiente y por qué? 



### 7. 7.	¿Usaste inteligencia artificial? Está bien si la usaste, pero indica cómo: qué herramienta y qué skills, instrucciones o configuraciones usaste, y para qué partes del trabajo

**Agente utilizado:** Kiro 

Se uso para el entendimiento general de la prueba técnica, para implementar las migraciones Liquibase desde CSV y configuración de seguridad, Se codifico gran parte del frontend con ayuda de la IA. además de revisión de arquitectura y solución de bugs, y finalmente en la implantación de los test


### 8. Implementación en Django (hipotético)

**Nivel de familiaridad:** 2/5 (proyecto pequeño realizado)

**Planteamiento:** algo asi

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

