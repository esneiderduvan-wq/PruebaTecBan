package com.ban.brp.service;

import com.ban.brp.dto.OfertaRequest;
import com.ban.brp.dto.OfertaResponse;
import com.ban.brp.dto.OferenteRequest;
import com.ban.brp.dto.PagoRequest;
import com.ban.brp.entity.*;
import com.ban.brp.exception.BusinessException;
import com.ban.brp.exception.ResourceNotFoundException;
import com.ban.brp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfertaServiceTest {

    @Mock
    private OfertaRepository ofertaRepository;

    @Mock
    private BrpRepository brpRepository;

    @Mock
    private OferenteRepository oferenteRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OfertaService ofertaService;

    private Brp brpDisponible;
    private Usuario radicador;
    private Usuario analista;
    private Oferta oferta;

    @BeforeEach
    void setUp() {
        brpDisponible = Brp.builder()
                .codigo("BRP-0001")
                .estado(EstadoBrp.DISPONIBLE)
                .tipoBrp(TipoBrp.APARTAMENTO)
                .departamento("Antioquia")
                .municipio("Medellín")
                .descripcion("Apartamento de prueba")
                .precio(BigDecimal.valueOf(320000000))
                .anioIngesta(2026)
                .mesIngesta(11)
                .diaIngesta(10)
                .build();

        radicador = Usuario.builder()
                .id(1L)
                .username("radicador1")
                .password("encodedPassword")
                .nombre("Juan")
                .apellido("Radicador")
                .email("juan@test.com")
                .rol(Rol.RADICADOR)
                .activo(true)
                .build();

        analista = Usuario.builder()
                .id(2L)
                .username("analista1")
                .password("encodedPassword")
                .nombre("María")
                .apellido("Analista")
                .email("maria@test.com")
                .rol(Rol.ANALISTA)
                .activo(true)
                .build();

        oferta = Oferta.builder()
                .id(1L)
                .brp(brpDisponible)
                .estado(EstadoOferta.RADICADO)
                .radicadoPor(radicador)
                .valorTotal(BigDecimal.valueOf(300000000))
                .descuento(BigDecimal.valueOf(6.25))
                .oferentes(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("radicarOferta creates offer successfully")
    void radicarOferta_createsOfferSuccessfully() {
        // Arrange
        OfertaRequest request = createOfertaRequest();
        when(brpRepository.findByCodigo("BRP-0001")).thenReturn(Optional.of(brpDisponible));
        when(ofertaRepository.existsByBrpCodigo("BRP-0001")).thenReturn(false);
        when(usuarioRepository.findByUsername("radicador1")).thenReturn(Optional.of(radicador));
        when(ofertaRepository.save(any(Oferta.class))).thenAnswer(inv -> {
            Oferta o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        // Act
        OfertaResponse result = ofertaService.radicarOferta(request, "radicador1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEstado()).isEqualTo(EstadoOferta.RADICADO);
        assertThat(result.getBrpCodigo()).isEqualTo("BRP-0001");
        verify(ofertaRepository).save(any(Oferta.class));
    }

    @Test
    @DisplayName("radicarOferta throws exception when BRP not available")
    void radicarOferta_throwsExceptionWhenBrpNotAvailable() {
        // Arrange
        Brp brpNoDisponible = Brp.builder()
                .codigo("BRP-0012")
                .estado(EstadoBrp.DADO_DE_BAJA)
                .build();

        OfertaRequest request = createOfertaRequest();
        when(brpRepository.findByCodigo("BRP-0001")).thenReturn(Optional.of(brpNoDisponible));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.radicarOferta(request, "radicador1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DISPONIBLE");
    }

    @Test
    @DisplayName("radicarOferta throws exception when BRP already has offer")
    void radicarOferta_throwsExceptionWhenBrpHasOffer() {
        // Arrange
        OfertaRequest request = createOfertaRequest();
        when(brpRepository.findByCodigo("BRP-0001")).thenReturn(Optional.of(brpDisponible));
        when(ofertaRepository.existsByBrpCodigo("BRP-0001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.radicarOferta(request, "radicador1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene una oferta asociada");
    }

    @Test
    @DisplayName("radicarOferta throws exception when more than 5 oferentes")
    void radicarOferta_throwsExceptionWhenMoreThan5Oferentes() {
        // Arrange
        java.util.List<OferenteRequest> oferentes = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            oferentes.add(createOferenteRequest());
        }
        OfertaRequest request = OfertaRequest.builder()
                .brpCodigo("BRP-0001")
                .oferentes(oferentes)
                .build();

        when(brpRepository.findByCodigo("BRP-0001")).thenReturn(Optional.of(brpDisponible));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.radicarOferta(request, "radicador1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Máximo 5 oferentes");
    }

    // NOTA: El servicio calcula automáticamente el porcentaje a partir de los valores de los pagos,
    // por lo que no es necesario validar que los porcentajes sumen 100
    // Esto se mantiene por compatibilidad con el DTO, pero no se usa en la lógica de negocio

    @Test
    @DisplayName("tomarOferta transitions from RADICADO to EN_PROCESO")
    void tomarOferta_transitionsFromRadicadoToEnProceso() {
        // Arrange
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(usuarioRepository.findByUsername("analista1")).thenReturn(Optional.of(analista));
        when(ofertaRepository.save(any(Oferta.class))).thenReturn(oferta);

        // Act
        OfertaResponse result = ofertaService.tomarOferta(1L, "analista1");

        // Assert
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.EN_PROCESO);
        assertThat(oferta.getAnalista()).isEqualTo(analista);
        verify(ofertaRepository).save(oferta);
    }

    @Test
    @DisplayName("tomarOferta throws exception when not in RADICADO state")
    void tomarOferta_throwsExceptionWhenNotInRadocadoState() {
        // Arrange
        oferta.setEstado(EstadoOferta.EN_PROCESO);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.tomarOferta(1L, "analista1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RADICADO");
    }

    @Test
    @DisplayName("tomarOferta throws exception when analyst is same as radicador")
    void tomarOferta_throwsExceptionWhenAnalistIsSameAsRadicador() {
        // Arrange - oferta is already set up with radicador as radicadoPor
        // We need to use "analista1" username but the oferta.getRadicadoPor() has radicador (id=1)
        // When username is "analista1", it returns analista (id=2), but that's wrong for this test
        // We need to mock it to return radicador when "radicador1" is queried
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(usuarioRepository.findByUsername("radicador1")).thenReturn(Optional.of(radicador));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.tomarOferta(1L, "radicador1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No puede tomar una oferta");
    }

    @Test
    @DisplayName("escalarOferta transitions from EN_PROCESO to ESCALADO")
    void escalarOferta_transitionsFromEnProcesoToEscalado() {
        // Arrange
        oferta.setEstado(EstadoOferta.EN_PROCESO);
        oferta.setAnalista(analista);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(ofertaRepository.save(any(Oferta.class))).thenReturn(oferta);

        // Act
        OfertaResponse result = ofertaService.escalarOferta(1L, "analista1");

        // Assert
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ESCALADO);
        verify(ofertaRepository).save(oferta);
    }

    @Test
    @DisplayName("aprobarOferta transitions from ESCALADO to APROBADO")
    void aprobarOferta_transitionsFromEscaladoToAprobado() {
        // Arrange
        oferta.setEstado(EstadoOferta.ESCALADO);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(ofertaRepository.save(any(Oferta.class))).thenReturn(oferta);

        // Act
        OfertaResponse result = ofertaService.aprobarOferta(1L, "jefe1");

        // Assert
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.APROBADO);
        assertThat(oferta.getFechaFinalizacion()).isNotNull();
    }

    @Test
    @DisplayName("aprobarOferta throws exception when not in ESCALADO state")
    void aprobarOferta_throwsExceptionWhenNotInEscaladoState() {
        // Arrange
        oferta.setEstado(EstadoOferta.EN_PROCESO);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.aprobarOferta(1L, "jefe1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESCALADO");
    }

    @Test
    @DisplayName("devolverOferta transitions from ESCALADO back to EN_PROCESO")
    void devolverOferta_transitionsFromEscaladoToEnProceso() {
        // Arrange
        oferta.setEstado(EstadoOferta.ESCALADO);
        oferta.setAnalista(analista);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));
        when(ofertaRepository.save(any(Oferta.class))).thenReturn(oferta);

        // Act
        OfertaResponse result = ofertaService.devolverOferta(1L, "jefe1", "Falta documentación");

        // Assert
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.EN_PROCESO);
        assertThat(oferta.getFechaDevolucion()).isNotNull();
    }

    @Test
    @DisplayName("devolverOferta throws exception when no analyst assigned")
    void devolverOferta_throwsExceptionWhenNoAnalystAssigned() {
        // Arrange
        oferta.setEstado(EstadoOferta.ESCALADO);
        when(ofertaRepository.findById(1L)).thenReturn(Optional.of(oferta));

        // Act & Assert
        assertThatThrownBy(() -> ofertaService.devolverOferta(1L, "jefe1", "Motivo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no tiene analista asignado");
    }

    // Helper methods
    private OfertaRequest createOfertaRequest() {
        OferenteRequest oferenteRequest = createOferenteRequest();
        return OfertaRequest.builder()
                .brpCodigo("BRP-0001")
                .oferentes(Arrays.asList(oferenteRequest))
                .build();
    }

    private OferenteRequest createOferenteRequest() {
        PagoRequest pagoRequest = PagoRequest.builder()
                .tipoPago(TipoPago.CONTADO)
                .fecha(LocalDate.of(2026, 11, 20))
                .valor(BigDecimal.valueOf(300000000))
                .build();

        return OferenteRequest.builder()
                .tipoIdentificacion(TipoIdentificacion.CC)
                .numeroIdentificacion("12345678")
                .nombre("Juan Pérez")
                .porcentajeParticipacion(BigDecimal.valueOf(100.00))
                .email("juan@email.com")
                .pagos(Arrays.asList(pagoRequest))
                .build();
    }
}