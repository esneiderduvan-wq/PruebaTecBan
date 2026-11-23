package com.ban.brp.controller;

import com.ban.brp.entity.Adjunto;
import com.ban.brp.service.AdjuntoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/adjuntos")
@RequiredArgsConstructor
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    @PostMapping(value = "/upload/{ofertaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RADICADOR')")
    public Adjunto upload(
            @PathVariable Long ofertaId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("nombre") String nombre) throws IOException {
        return adjuntoService.uploadAdjunto(ofertaId, file, nombre);
    }

    @GetMapping("/oferta/{ofertaId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public List<Adjunto> getByOferta(@PathVariable Long ofertaId) {
        return adjuntoService.getAdjuntosByOferta(ofertaId);
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE', 'ANALISTA', 'RADICADOR')")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws MalformedURLException {
        Adjunto adjunto = adjuntoService.getAdjuntoById(id);
        Path filePath = Paths.get(adjunto.getRutaLocal());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + adjunto.getNombreArchivo() + "\"")
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adjuntoService.deleteAdjunto(id);
        return ResponseEntity.noContent().build();
    }
}