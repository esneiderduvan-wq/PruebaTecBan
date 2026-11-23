package com.ban.brp.service;

import com.ban.brp.entity.Adjunto;
import com.ban.brp.entity.Oferta;
import com.ban.brp.exception.ResourceNotFoundException;
import com.ban.brp.repository.AdjuntoRepository;
import com.ban.brp.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjuntoService {

    private final AdjuntoRepository adjuntoRepository;
    private final OfertaRepository ofertaRepository;

    private static final String UPLOAD_DIR = "uploads/adjuntos";

    @Transactional
    public Adjunto uploadAdjunto(Long ofertaId, MultipartFile file, String nombre) throws IOException {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + ofertaId));

        // Crear directorio si no existe
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Sanitizar nombre de archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo");
        String sanitizedFilename = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        // Guardar archivo
        Path filePath = uploadPath.resolve(sanitizedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Crear registro en base de datos
        Adjunto adjunto = Adjunto.builder()
                .oferta(oferta)
                .nombreArchivo(originalFilename)
                .rutaLocal(filePath.toString())
                .fechaCarga(LocalDateTime.now())
                .build();

        if (nombre != null && !nombre.isBlank()) {
            adjunto.setDescripcion(nombre);
        }

        Adjunto saved = adjuntoRepository.save(adjunto);
        log.info("Adjunto {} guardado para oferta {}", saved.getId(), ofertaId);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Adjunto> getAdjuntosByOferta(Long ofertaId) {
        return adjuntoRepository.findByOfertaId(ofertaId);
    }

    @Transactional(readOnly = true)
    public Adjunto getAdjuntoById(Long id) {
        return adjuntoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado: " + id));
    }

    @Transactional
    public void deleteAdjunto(Long id) {
        Adjunto adjunto = getAdjuntoById(id);
        
        try {
            Files.deleteIfExists(Paths.get(adjunto.getRutaLocal()));
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico: {}",adjunto.getRutaLocal());
        }
        
        adjuntoRepository.delete(adjunto);
        log.info("Adjunto {} eliminado", id);
    }
}