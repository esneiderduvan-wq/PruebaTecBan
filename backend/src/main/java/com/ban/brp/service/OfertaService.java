package com.ban.brp.service;

import com.ban.brp.dto.*;
import com.ban.brp.entity.*;
import com.ban.brp.exception.BusinessException;
import com.ban.brp.exception.OptimisticLockException;
import com.ban.brp.exception.ResourceNotFoundException;
import com.ban.brp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final BrpRepository brpRepository;
    private final OferenteRepository oferenteRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdjuntoRepository adjuntoRepository;

    @Transactional(readOnly = true)
    public List<OfertaResponse> findAll() {
        return ofertaRepository.findAllOrderByFechaCreacionDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfertaResponse> findByCurrentUser(String username, String rol) {
        if ("RADICADOR".equals(rol)) {
            return ofertaRepository.findByRadicadoPorUsername(username).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            return ofertaRepository.findAllOrderByFechaCreacionDesc().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public List<OfertaResponse> findEscaladas() {
        return ofertaRepository.findEscaladas().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfertaResponse findById(Long id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));
        return toResponse(oferta);
    }

    @Transactional
    public OfertaResponse radicarOferta(OfertaRequest request, String username) {
        Brp brp = brpRepository.findByCodigo(request.getBrpCodigo())
                .orElseThrow(() -> new ResourceNotFoundException("BRP no encontrado: " + request.getBrpCodigo()));

        if (brp.getEstado() != EstadoBrp.DISPONIBLE) {
            throw new BusinessException("Solo se puede ofertar sobre BRP en estado DISPONIBLE");
        }

        if (ofertaRepository.existsByBrpCodigo(request.getBrpCodigo())) {
            throw new BusinessException("Este BRP ya tiene una oferta asociada");
        }
        if (request.getOferentes().size() > 5) {
            throw new BusinessException("Máximo 5 oferentes por oferta");
        }

        Usuario radicador = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));

        Oferta oferta = Oferta.builder()
                .brp(brp)
                .estado(EstadoOferta.RADICADO)
                .radicadoPor(radicador)
                .fechaCreacion(LocalDateTime.now())
                .build();

        BigDecimal valorTotal = BigDecimal.ZERO;
        List<BigDecimal> valoresOferentes = new ArrayList<>();

        // Primero calcular el valor total de la oferta sumando todos los pagos de todos los oferentes
        for (OferenteRequest oferenteReq : request.getOferentes()) {
            if (oferenteReq.getPagos().size() > 6) {
                throw new BusinessException("Máximo 6 pagos por oferente");
            }

            BigDecimal pagosOferente = BigDecimal.ZERO;
            for (PagoRequest pagoReq : oferenteReq.getPagos()) {
                pagosOferente = pagosOferente.add(pagoReq.getValor());
            }
            valoresOferentes.add(pagosOferente);
            valorTotal = valorTotal.add(pagosOferente);
        }

        if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El valor total de la oferta debe ser mayor a 0");
        }

        // Ahora crear los oferentes calculando su porcentaje automáticamente
        int i = 0;
        for (OferenteRequest oferenteReq : request.getOferentes()) {
            BigDecimal valorOferente = valoresOferentes.get(i);
            // Calcular porcentaje: (valorOferente / valorTotal) * 100
            BigDecimal porcentaje = valorOferente.multiply(new BigDecimal("100"))
                    .divide(valorTotal, 2, RoundingMode.HALF_UP);

            Oferente oferente = Oferente.builder()
                    .oferta(oferta)
                    .tipoIdentificacion(oferenteReq.getTipoIdentificacion())
                    .numeroIdentificacion(oferenteReq.getNumeroIdentificacion())
                    .nombre(oferenteReq.getNombre())
                    .porcentajeParticipacion(porcentaje) // Calculado automáticamente
                    .email(oferenteReq.getEmail())
                    .telefono(oferenteReq.getTelefono())
                    .build();

            for (PagoRequest pagoReq : oferenteReq.getPagos()) {
                Pago pago = Pago.builder()
                        .oferente(oferente)
                        .tipoPago(pagoReq.getTipoPago())
                        .fecha(pagoReq.getFecha())
                        .valor(pagoReq.getValor())
                        .entidadFinanciadora(pagoReq.getEntidadFinanciadora())
                        .build();
                oferente.getPagos().add(pago);
            }

            oferta.getOferentes().add(oferente);
            i++;
        }

        BigDecimal descuento = calcularDescuento(brp.getPrecio(), valorTotal);

        oferta.setValorTotal(valorTotal);
        oferta.setDescuento(descuento);

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta radicada: {} por usuario: {}", saved.getId(), username);

        return toResponse(saved);
    }

    @Transactional
    public OfertaResponse tomarOferta(Long id, String username) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));

        if (oferta.getEstado() != EstadoOferta.RADICADO) {
            throw new BusinessException("Solo se pueden tomar ofertas en estado RADICADO");
        }

        Usuario analista = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (oferta.getRadicadoPor().getId().equals(analista.getId())) {
            throw new BusinessException("No puede tomar una oferta que usted stesso radicó");
        }

        oferta.setEstado(EstadoOferta.EN_PROCESO);
        oferta.setAnalista(analista);
        oferta.setFechaModificacion(LocalDateTime.now());

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta {} tomada por analista: {}", id, username);

        return toResponse(saved);
    }

    @Transactional
    public OfertaResponse escalarOferta(Long id, String username) {
        Oferta oferta = getOfertaConValidacion(id, username, EstadoOferta.EN_PROCESO);

        oferta.setEstado(EstadoOferta.ESCALADO);
        oferta.setFechaEscalamiento(LocalDateTime.now());
        oferta.setFechaModificacion(LocalDateTime.now());

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta {} escalada por analista: {}", id, username);

        return toResponse(saved);
    }

    @Transactional
    public OfertaResponse aprobarOferta(Long id, String username) {
        Oferta oferta = getOfertaConValidacion(id, username, EstadoOferta.ESCALADO);

        oferta.setEstado(EstadoOferta.APROBADO);
        oferta.setFechaFinalizacion(LocalDateTime.now());
        oferta.setFechaModificacion(LocalDateTime.now());

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta {} aprobada por jefe: {}", id, username);

        return toResponse(saved);
    }

    @Transactional
    public OfertaResponse rechazarOferta(Long id, String username, String reason) {
        Oferta oferta = getOfertaConValidacion(id, username, null);

        boolean esAnalista = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANALISTA"));
        boolean esJefe = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JEFE"));

        if (esAnalista && oferta.getEstado() == EstadoOferta.EN_PROCESO) {
        } else if (esJefe && oferta.getEstado() == EstadoOferta.ESCALADO) {
        } else {
            throw new BusinessException("No tiene permisos para rechazar esta oferta en su estado actual");
        }

        // Liberar el BRP para que pueda recibir nuevas ofertas
        oferta.getBrp().setEstado(EstadoBrp.DISPONIBLE);

        oferta.setEstado(EstadoOferta.RECHAZADO);
        oferta.setFechaFinalizacion(LocalDateTime.now());
        oferta.setFechaModificacion(LocalDateTime.now());

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta {} rechazada por: {}. BRP {} liberado.", id, username, oferta.getBrp().getCodigo());

        return toResponse(saved);
    }

    @Transactional
    public OfertaResponse devolverOferta(Long id, String username, String reason) {
        Oferta oferta = getOfertaConValidacion(id, username, EstadoOferta.ESCALADO);

        if (oferta.getAnalista() == null) {
            throw new BusinessException("La oferta no tiene analista asignado");
        }

        if (oferta.getAnalista().getUsername().equals(username)) {
            throw new BusinessException("No puede devolver la oferta al mismo analista");
        }

        oferta.setEstado(EstadoOferta.EN_PROCESO);
        oferta.setFechaDevolucion(LocalDateTime.now());
        oferta.setMotivoDevolucion(reason);
        oferta.setFechaModificacion(LocalDateTime.now());

        Oferta saved = ofertaRepository.save(oferta);
        log.info("Oferta {} devuelta a analista por: {}. Motivo: {}", id, username, reason);

        return toResponse(saved);
    }

    private Oferta getOfertaConValidacion(Long id, String username, EstadoOferta estadoRequerido) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada: " + id));

        if (estadoRequerido != null && oferta.getEstado() != estadoRequerido) {
            throw new BusinessException("La oferta debe estar en estado " + estadoRequerido + " para esta operación");
        }

        return oferta;
    }

    private BigDecimal calcularDescuento(BigDecimal precio, BigDecimal valorOferta) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return precio.subtract(valorOferta)
                .divide(precio, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private OfertaResponse toResponse(Oferta oferta) {
        List<OferenteResponse> oferenteResponses = new ArrayList<>();
        for (Oferente oferente : oferta.getOferentes()) {
            List<PagoResponse> pagoResponses = new ArrayList<>();
            for (Pago pago : oferente.getPagos()) {
                pagoResponses.add(PagoResponse.builder()
                        .id(pago.getId())
                        .tipoPago(pago.getTipoPago())
                        .fecha(pago.getFecha())
                        .valor(pago.getValor())
                        .entidadFinanciadora(pago.getEntidadFinanciadora())
                        .build());
            }
            oferenteResponses.add(OferenteResponse.builder()
                    .id(oferente.getId())
                    .tipoIdentificacion(oferente.getTipoIdentificacion())
                    .numeroIdentificacion(oferente.getNumeroIdentificacion())
                    .nombre(oferente.getNombre())
                    .porcentajeParticipacion(oferente.getPorcentajeParticipacion())
                    .email(oferente.getEmail())
                    .telefono(oferente.getTelefono())
                    .pagos(pagoResponses)
                    .build());
        }

        return OfertaResponse.builder()
                .id(oferta.getId())
                .brpCodigo(oferta.getBrp().getCodigo())
                .brpDescripcion(oferta.getBrp().getDescripcion())
                .valorTotal(oferta.getValorTotal())
                .descuento(oferta.getDescuento())
                .estado(oferta.getEstado())
                .radicadoPor(oferta.getRadicadoPor().getUsername())
                .analista(oferta.getAnalista() != null ? oferta.getAnalista().getUsername() : null)
                .fechaCreacion(oferta.getFechaCreacion())
                .fechaModificacion(oferta.getFechaModificacion())
                .fechaEscalamiento(oferta.getFechaEscalamiento())
                .fechaDevolucion(oferta.getFechaDevolucion())
                .fechaFinalizacion(oferta.getFechaFinalizacion())
                .motivoDevolucion(oferta.getMotivoDevolucion())
                .oferentes(oferenteResponses)
                .totalAdjuntos((int) adjuntoRepository.countByOfertaId(oferta.getId()))
                .tieneAdjuntos(adjuntoRepository.countByOfertaId(oferta.getId()) > 0)
                .build();
    }
}