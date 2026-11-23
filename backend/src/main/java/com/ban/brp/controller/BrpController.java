package com.ban.brp.controller;

import com.ban.brp.dto.BrpResponse;
import com.ban.brp.dto.CatalogUpdateResponse;
import com.ban.brp.entity.EstadoBrp;
import com.ban.brp.service.BrpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brp")
@RequiredArgsConstructor
public class BrpController {

    private final BrpService brpService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<List<BrpResponse>> findAll(
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        // RADICADOR solo ve disponibles
        if ("RADICADOR".equals(rol)) {
            return ResponseEntity.ok(brpService.findAllDisponibles());
        }
        return ResponseEntity.ok(brpService.findAll());
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<List<BrpResponse>> findDisponibles() {
        return ResponseEntity.ok(brpService.findAllDisponibles());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<BrpResponse>> findAllWithHistorial() {
        return ResponseEntity.ok(brpService.findAllWithHistorial());
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<BrpResponse> findByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(brpService.findByCodigo(codigo));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA')")
    public ResponseEntity<List<BrpResponse>> findByEstado(@PathVariable EstadoBrp estado) {
        return ResponseEntity.ok(brpService.findByEstado(estado));
    }

    @GetMapping("/catalogo/ultima-actualizacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<CatalogUpdateResponse> getCatalogoInfo() {
        return ResponseEntity.ok(brpService.getCatalogoUpdateInfo());
    }

    @GetMapping("/{codigo}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA')")
    public ResponseEntity<List<BrpResponse>> getBrpHistorial(@PathVariable String codigo) {
        return ResponseEntity.ok(brpService.findBrpHistorial(codigo));
    }
}