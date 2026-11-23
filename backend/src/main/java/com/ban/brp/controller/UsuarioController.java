package com.ban.brp.controller;

import com.ban.brp.dto.UsuarioRequest;
import com.ban.brp.dto.UsuarioResponse;
import com.ban.brp.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> findAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(
            @Valid @RequestBody UsuarioRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long createdBy = null;
        if (userDetails instanceof com.ban.brp.security.CustomUserDetails) {
            createdBy = ((com.ban.brp.security.CustomUserDetails) userDetails).getId();
        }
        UsuarioResponse response = usuarioService.create(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        usuarioService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        usuarioService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/modulos")
    public ResponseEntity<Void> assignModulo(
            @PathVariable Long id,
            @RequestParam String modulo,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long asignadoPor = null;
        if (userDetails instanceof com.ban.brp.security.CustomUserDetails) {
            asignadoPor = ((com.ban.brp.security.CustomUserDetails) userDetails).getId();
        }
        usuarioService.assignModulo(id, modulo, asignadoPor);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/modulos")
    public ResponseEntity<Void> removeModulo(
            @PathVariable Long id,
            @RequestParam String modulo) {
        usuarioService.removeModulo(id, modulo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/modulos")
    public ResponseEntity<List<String>> getModulos(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.getModulosByUsuario(id));
    }
}