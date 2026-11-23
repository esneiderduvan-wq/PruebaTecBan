package com.ban.brp.service;

import com.ban.brp.dto.BrpResponse;
import com.ban.brp.dto.CatalogUpdateResponse;
import com.ban.brp.entity.Brp;
import com.ban.brp.entity.EstadoBrp;
import com.ban.brp.exception.ResourceNotFoundException;
import com.ban.brp.repository.BrpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrpService {

    private final BrpRepository brpRepository;

    @Transactional(readOnly = true)
    public List<BrpResponse> findAll() {
        return brpRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrpResponse> findAllWithHistorial() {
        return brpRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrpResponse> findAllDisponibles() {
        return brpRepository.findAllDisponibles().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BrpResponse findByCodigo(String codigo) {
        Brp brp = brpRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("BRP no encontrado: " + codigo));
        return toResponse(brp);
    }

    @Transactional(readOnly = true)
    public CatalogUpdateResponse getCatalogoUpdateInfo() {
        Integer maxFecha = brpRepository.findMaxFechaIngesta();
        
        if (maxFecha == null) {
            return CatalogUpdateResponse.builder()
                    .ultimaActualizacion(null)
                    .fechaFormateada(null)
                    .totalBrp(0)
                    .disponibles(0)
                    .build();
        }

        int anio = maxFecha / 10000;
        int mes = (maxFecha % 10000) / 100;
        int dia = maxFecha % 100;

        List<Brp> brps = brpRepository.findByFechaIngesta(maxFecha);
        long totalBrp = brpRepository.count();
        long disponibles = brpRepository.findByEstado(EstadoBrp.DISPONIBLE).size();

        return CatalogUpdateResponse.builder()
                .ultimaActualizacion(java.time.LocalDateTime.of(anio, mes, dia, 0, 0))
                .fechaFormateada(java.time.LocalDate.of(anio, mes, dia))
                .totalBrp((int) totalBrp)
                .disponibles((int) disponibles)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BrpResponse> findByEstado(EstadoBrp estado) {
        return brpRepository.findByEstado(estado).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrpResponse> findBrpHistorial(String codigo) {
        return brpRepository.findByCodigoOrderByFechaIngestaDesc(codigo).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BrpResponse toResponse(Brp brp) {
        return BrpResponse.builder()
                .codigo(brp.getCodigo())
                .estado(brp.getEstado())
                .tipoBrp(brp.getTipoBrp())
                .matriculaInmobiliaria(brp.getMatriculaInmobiliaria())
                .placa(brp.getPlaca())
                .departamento(brp.getDepartamento())
                .municipio(brp.getMunicipio())
                .descripcion(brp.getDescripcion())
                .precio(brp.getPrecio())
                .fechaAsignacionPrecio(brp.getFechaAsignacionPrecio())
                .anioIngesta(brp.getAnioIngesta())
                .mesIngesta(brp.getMesIngesta())
                .diaIngesta(brp.getDiaIngesta())
                .ultimaActualizacion(brp.getUltimaActualizacion())
                .build();
    }
}