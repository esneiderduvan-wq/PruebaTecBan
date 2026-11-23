package com.ban.brp.controller;

import com.ban.brp.dto.OfertaRequest;
import com.ban.brp.dto.OfertaResponse;
import com.ban.brp.entity.Rol;
import com.ban.brp.service.OfertaService;
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
@RequestMapping("/api/ofertas")
@RequiredArgsConstructor
public class OfertaController {

    private final OfertaService ofertaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<List<OfertaResponse>> findAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        String rol = null;
        if (userDetails instanceof com.ban.brp.security.CustomUserDetails) {
            rol = ((com.ban.brp.security.CustomUserDetails) userDetails).getRol();
        }
        return ResponseEntity.ok(ofertaService.findByCurrentUser(
                userDetails.getUsername(), rol));
    }

    @GetMapping("/escaladas")
    @PreAuthorize("hasRole('JEFE')")
    public ResponseEntity<List<OfertaResponse>> findEscaladas() {
        return ResponseEntity.ok(ofertaService.findEscaladas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<OfertaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ofertaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('RADICADOR')")
    public ResponseEntity<OfertaResponse> radicar(
            @Valid @RequestBody OfertaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OfertaResponse response = ofertaService.radicarOferta(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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