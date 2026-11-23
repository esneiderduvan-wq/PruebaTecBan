package com.ban.brp.service;

import com.ban.brp.dto.UsuarioRequest;
import com.ban.brp.dto.UsuarioResponse;
import com.ban.brp.entity.AuditoriaModulo;
import com.ban.brp.entity.Modulo;
import com.ban.brp.entity.Rol;
import com.ban.brp.entity.Usuario;
import com.ban.brp.entity.UsuarioModulo;
import com.ban.brp.exception.BusinessException;
import com.ban.brp.exception.ResourceNotFoundException;
import com.ban.brp.repository.AuditoriaModuloRepository;
import com.ban.brp.repository.ModuloRepository;
import com.ban.brp.repository.UsuarioModuloRepository;
import com.ban.brp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioModuloRepository usuarioModuloRepository;
    private final ModuloRepository moduloRepository;
    private final AuditoriaModuloRepository auditoriaModuloRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> MODULOS_ADMIN = List.of("CATALOGO", "SEGUIMIENTO", "ADMINISTRACION");
    private static final List<String> MODULOS_RADICADOR = List.of("RADICACION", "SEGUIMIENTO", "CATALOGO");
    private static final List<String> MODULOS_ANALISTA = List.of("ANALISIS", "SEGUIMIENTO", "CATALOGO");
    private static final List<String> MODULOS_JEFE = List.of("APROBACION", "SEGUIMIENTO", "CATALOGO");

    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest request, Long createdBy) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El username ya existe: " + request.getUsername());
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        Rol rolEnum;
        try {
            rolEnum = Rol.valueOf(request.getRol());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol inválido: " + request.getRol());
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido("") // Campo apellido no requerido en frontend
                .email(request.getEmail())
                .rol(rolEnum)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario creado: {} por usuario ID: {}", saved.getUsername(), createdBy);

        assignModulesByRole(saved, createdBy);

        return toResponse(saved);
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

        if (!usuario.getUsername().equals(request.getUsername()) &&
                usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El username ya existe: " + request.getUsername());
        }

        if (!usuario.getEmail().equals(request.getEmail()) &&
                usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está registrado");
        }

        Rol rolEnum;
        try {
            rolEnum = Rol.valueOf(request.getRol());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol inválido: " + request.getRol());
        }

        usuario.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        usuario.setNombre(request.getNombre());
        usuario.setApellido(""); // Campo apellido no requerido
        usuario.setEmail(request.getEmail());
        usuario.setRol(rolEnum);
        usuario.setUpdatedAt(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario actualizado: {}", saved.getUsername());

        usuarioModuloRepository.deleteAllByUsuarioId(id);
        assignModulesByRole(saved, saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

        usuario.setActivo(false);
        usuario.setUpdatedAt(LocalDateTime.now());

        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: {}", usuario.getUsername());
    }

    @Transactional
    public void activate(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

        usuario.setActivo(true);
        usuario.setUpdatedAt(LocalDateTime.now());

        usuarioRepository.save(usuario);
        log.info("Usuario activado: {}", usuario.getUsername());
    }

    @Transactional
    public void assignModulo(Long usuarioId, String moduloNombre, Long asignadoPor) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        Modulo modulo = moduloRepository.findByNombre(moduloNombre)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduloNombre));

        if (usuarioModuloRepository.existsByUsuarioIdAndModuloId(usuarioId, modulo.getId())) {
            throw new BusinessException("El usuario ya tiene asignado el módulo: " + moduloNombre);
        }

        if (moduloNombre.equals("ADMINISTRACION") && usuario.getUsername().equals("admin")) {
            throw new BusinessException("El administrador no puede quitarse su propio permiso de administración");
        }

        UsuarioModulo usuarioModulo = UsuarioModulo.builder()
                .usuario(usuario)
                .modulo(modulo)
                .asignadoPor(asignadoPor != null ? usuarioRepository.findById(asignadoPor).orElse(null) : null)
                .build();

        usuarioModuloRepository.save(usuarioModulo);

        registrarAuditoria(usuarioId, moduloNombre, "ASIGNADO", asignadoPor);

        log.info("Módulo {} asignado a usuario {} por usuario ID: {}", moduloNombre, usuario.getUsername(), asignadoPor);
    }

    @Transactional
    public void removeModulo(Long usuarioId, String moduloNombre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        Modulo modulo = moduloRepository.findByNombre(moduloNombre)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + moduloNombre));

        if (moduloNombre.equals("ADMINISTRACION") && usuario.getUsername().equals("admin")) {
            throw new BusinessException("El administrador no puede quitarse su propio permiso de administración");
        }

        if (moduloNombre.equals("ADMINISTRACION")) {
            long adminCount = usuarioModuloRepository.countUsuariosByModulo("ADMINISTRACION");
            if (adminCount <= 1) {
                throw new BusinessException("El sistema no puede quedarse sin administradores");
            }
        }

        usuarioModuloRepository.deleteByUsuarioIdAndModuloId(usuarioId, modulo.getId());

        Long asignadoPor = getCurrentUserId();
        registrarAuditoria(usuarioId, moduloNombre, "REMOVIDO", asignadoPor);

        log.info("Módulo {} removido de usuario {}", moduloNombre, usuario.getUsername());
    }

    @Transactional(readOnly = true)
    public List<String> getModulosByUsuario(Long usuarioId) {
        return usuarioModuloRepository.findByUsuarioIdWithModulos(usuarioId).stream()
                .map(um -> um.getModulo().getNombre())
                .collect(Collectors.toList());
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        List<String> modulos = usuarioModuloRepository.findByUsuarioIdWithModulos(usuario.getId()).stream()
                .map(um -> um.getModulo().getNombre())
                .collect(Collectors.toList());

        return UsuarioResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .activo(usuario.getActivo())
                .rol(usuario.getRol().name())
                .createdAt(usuario.getCreatedAt())
                .modulos(modulos)
                .build();
    }

    private void assignModulesByRole(Usuario usuario, Long asignadoPor) {
        List<String> modulos = switch (usuario.getRol()) {
            case ADMINISTRADOR -> MODULOS_ADMIN;
            case RADICADOR -> MODULOS_RADICADOR;
            case ANALISTA -> MODULOS_ANALISTA;
            case JEFE -> MODULOS_JEFE;
        };

        for (String moduloNombre : modulos) {
            moduloRepository.findByNombre(moduloNombre).ifPresent(modulo -> {
                if (!usuarioModuloRepository.existsByUsuarioIdAndModuloId(usuario.getId(), modulo.getId())) {
                    UsuarioModulo usuarioModulo = UsuarioModulo.builder()
                            .usuario(usuario)
                            .modulo(modulo)
                            .asignadoPor(asignadoPor != null ? usuarioRepository.findById(asignadoPor).orElse(null) : null)
                            .build();
                    usuarioModuloRepository.save(usuarioModulo);
                }
            });
        }
        log.info("Módulos asignados automáticamente a usuario {} según rol {}", usuario.getUsername(), usuario.getRol());
    }

    private void registrarAuditoria(Long usuarioId, String moduloNombre, String accion, Long asignadoPor) {
        AuditoriaModulo auditoria = AuditoriaModulo.builder()
                .usuarioId(usuarioId)
                .moduloNombre(moduloNombre)
                .accion(accion)
                .asignadoPor(asignadoPor)
                .fecha(LocalDateTime.now())
                .build();
        auditoriaModuloRepository.save(auditoria);
        log.info("Auditoría: Módulo {} {} para usuario {} por usuario ID: {}", moduloNombre, accion, usuarioId, asignadoPor);
    }

    private Long getCurrentUserId() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.ban.brp.security.CustomUserDetails) {
                return ((com.ban.brp.security.CustomUserDetails) authentication.getPrincipal()).getId();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener el usuario actual del contexto de seguridad");
        }
        return null;
    }
}