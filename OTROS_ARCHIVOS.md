# Documentación Técnica Interna

Este archivo NO se entrega con la prueba. Es solo para referencia del desarrollador.

## Lombok

### ¿Qué es Lombok?

Lombok es una biblioteca de Java que reduce el código repetitivo (boilerplate) mediante anotaciones que generan automáticamente métodos como `getters`, `setters`, `constructors`, `toString`, `equals`, `hashCode`, y más en tiempo de compilación.

### ¿Por qué usarlo?

**Sin Lombok:**
```java
public class Usuario {
    private Long id;
    private String username;
    private String password;
    private String nombre;
    
    public Usuario() {}
    
    public Usuario(Long id, String username, String password, String nombre) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nombre = nombre;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    // ... más getters/setters
    
    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username='" + username + "'}";
    }
}
```

**Con Lombok:**
```java
@Data  // Genera getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Constructor sin argumentos
@AllArgsConstructor  // Constructor con todos los argumentos
public class Usuario {
    private Long id;
    private String username;
    private String password;
    private String nombre;
}
```

### Anotaciones usadas en este proyecto

| Anotación | Descripción |
|-----------|-------------|
| `@Data` | Genera `getter`, `setter`, `toString`, `equals`, `hashCode` |
| `@NoArgsConstructor` | Genera constructor sin argumentos |
| `@AllArgsConstructor` | Genera constructor con todos los argumentos |
| `@Builder` | Implementa el patrón Builder para creación de objetos |
| `@Getter` / `@Setter` | Genera solo getter o setter |

### Configuración en build.gradle

```groovy
plugins {
    id 'io.freefair.lombok' version '8.4'
}

lombok {
    sourceSets = [sourceSets.main]
}
```

### Advertencia para IDE

Al usar Lombok, tu IDE necesita el plugin de Lombok para reconocer las anotaciones:
- **IntelliJ IDEA**: Instalar desde Marketplace
- **Eclipse**: Agregar `-javaagent:lombok.jar` en VM arguments

---

## MapStruct

### ¿Qué es MapStruct?

MapStruct es una biblioteca de mapeo objeto-objeto (object-to-object mapping) que genera automáticamente código para convertir entre diferentes objetos, especialmente útil para convertir entre entidades JPA y DTOs.

### ¿Por qué usarlo?

**Sin MapStruct:**
```java
public BrpResponse toResponse(Brp brp) {
    BrpResponse response = new BrpResponse();
    response.setCodigo(brp.getCodigo());
    response.setEstado(brp.getEstado());
    response.setTipoBrp(brp.getTipoBrp());
    // ... asignar cada campo manualmente
    return response;
}
```

**Con MapStruct:**
```java
@Mapper(componentModel = "spring")
public interface BrpMapper {
    BrpResponse toResponse(Brp brp);
}
```

### Diferencias con Lombok

| Aspecto | Lombok | MapStruct |
|---------|--------|-----------|
| **Propósito** | Reduce boilerplate en la misma clase | Mapea entre clases diferentes |
| **Generación** | Métodos en la misma clase | Clases de mapeo separadas |
| **Compilación** | Modifica el bytecode de la clase original | Genera clases Mapper independientes |
| **Rendimiento** | Runtime overhead mínimo | Compila a código nativo, sin overhead |

### Anotaciones usadas

| Anotación | Descripción |
|-----------|-------------|
| `@Mapper` | Define una interfaz como mapper |
| `@Mapping` | Configura mapeo de campos |
| `@BeanMapping` | Ignora campos null |

### Ejemplo de Mapper

```java
@Mapper(componentModel = "spring", uses = {PagoMapper.class})
public interface OfertaMapper {
    
    @Mapping(target = "brpCodigo", source = "brp.codigo")
    @Mapping(target = "radicadoPor", source = "radicadoPor.username")
    OfertaResponse toResponse(Oferta oferta);
}
```

### Configuración en build.gradle

```groovy
dependencies {
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
}

annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
```

### Procesamiento de Anotaciones

MapStruct necesita que su processor corra antes que Lombok. El orden se configura automáticamente por el plugin de Lombok, pero en build.gradle se puede asegurar:

```groovy
compileJava {
    options.annotationProcessorPath = configurations.annotationProcessor
}
```

---

## Uso Combinado de Lombok y MapStruct

Cuando se usan juntos (muy común), hay que tener cuidado con el orden de compilación:

```java
// Entity con Lombok
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brp {
    private String codigo;
    private EstadoBrp estado;
    // ...
}

// Mapper que usa MapStruct con la entity
@Mapper(componentModel = "spring")
public interface BrpMapper {
    BrpResponse toResponse(Brp brp);
}
```

**Orden de compilación:**
1. Lombok procesa las anotaciones en las entidades
2. MapStruct procesa los mappers, generando código que usa los getters/setters de Lombok

---

## Otras Dependencias del Proyecto

| Dependencia | Propósito |
|-------------|-----------|
| `jjwt-api` | JSON Web Token para autenticación |
| `liquibase-core` | Migraciones de base de datos versionadas |
| `postgresql` | Driver de PostgreSQL |
| `spring-boot-starter-validation` | Bean Validation (Jakarta) |
| `spring-boot-starter-security` | Seguridad con Spring Security |

---

## Configuración de application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/brp_db
    username: brp_user
    password: brp_password
  jpa:
    hibernate:
      ddl-auto: validate  # Solo valida, no modifica esquema

jwt:
  secret: brp-secret-key-para-jwt-debe-ser-muy-largo-mínimo-256-bits
  expiration: 28800000  # 8 horas
```

---

## Notas para el Desarrollador

1. **Orden de compilación:** Lombok debe compilarse antes que MapStruct. El plugin `io.freefair.lombok` maneja esto automáticamente.

2. **Debug de Mappers:** Si el mapper no funciona, verificar que la anotación `@Mapper(componentModel = "spring")` esté presente.

3. **Errores de compilación:** Al agregar Lombok, puede que el IDE muestre errores hasta que se reinicie con el plugin instalado.

4. **Rendimiento:** MapStruct genera código en tiempo de compilación, no tiene overhead runtime. Lombok añade mínimo overhead.

5. **Testing:** Las entidades con Lombok son difíciles de probar directamente porque los métodos se generan en compilación. Usar DTOs para pruebas.