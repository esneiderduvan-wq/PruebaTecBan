package com.ban.brp.service;

import com.ban.brp.dto.BrpResponse;
import com.ban.brp.dto.CatalogUpdateResponse;
import com.ban.brp.entity.Brp;
import com.ban.brp.entity.EstadoBrp;
import com.ban.brp.entity.TipoBrp;
import com.ban.brp.repository.BrpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrpServiceTest {

    @Mock
    private BrpRepository brpRepository;

    @InjectMocks
    private BrpService brpService;

    private Brp brp1;
    private Brp brp2;

    @BeforeEach
    void setUp() {
        brp1 = Brp.builder()
                .codigo("BRP-0001")
                .estado(EstadoBrp.DISPONIBLE)
                .tipoBrp(TipoBrp.APARTAMENTO)
                .matriculaInmobiliaria("001-554854")
                .departamento("Antioquia")
                .municipio("Medellín")
                .descripcion("Apartamento en Medellín")
                .precio(BigDecimal.valueOf(320000000))
                .fechaAsignacionPrecio(LocalDate.of(2026, 9, 20))
                .anioIngesta(2026)
                .mesIngesta(11)
                .diaIngesta(10)
                .build();

        brp2 = Brp.builder()
                .codigo("BRP-0002")
                .estado(EstadoBrp.SIN_PRECIO)
                .tipoBrp(TipoBrp.CASA)
                .matriculaInmobiliaria("50C-688670")
                .departamento("Cundinamarca")
                .municipio("Bogotá")
                .descripcion("Casa en Bogotá")
                .anioIngesta(2026)
                .mesIngesta(11)
                .diaIngesta(10)
                .build();
    }

    @Test
    @DisplayName("findAll returns list of all BRPs")
    void findAll_returnsAllBrps() {
        when(brpRepository.findAll()).thenReturn(Arrays.asList(brp1, brp2));

        List<BrpResponse> result = brpService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCodigo()).isEqualTo("BRP-0001");
        assertThat(result.get(1).getCodigo()).isEqualTo("BRP-0002");
    }

    @Test
    @DisplayName("findAllDisponibles returns only DISPONIBLE BRPs")
    void findAllDisponibles_returnsOnlyDisponibles() {
        when(brpRepository.findAllDisponibles()).thenReturn(Arrays.asList(brp1));

        List<BrpResponse> result = brpService.findAllDisponibles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstado()).isEqualTo(EstadoBrp.DISPONIBLE);
    }

    @Test
    @DisplayName("findByCodigo returns BRP when exists")
    void findByCodigo_returnsBrpWhenExists() {
        when(brpRepository.findByCodigo("BRP-0001")).thenReturn(java.util.Optional.of(brp1));

        BrpResponse result = brpService.findByCodigo("BRP-0001");

        assertThat(result).isNotNull();
        assertThat(result.getCodigo()).isEqualTo("BRP-0001");
        assertThat(result.getPrecio()).isEqualTo(BigDecimal.valueOf(320000000));
    }

    @Test
    @DisplayName("getCatalogoUpdateInfo returns correct update date")
    void getCatalogoUpdateInfo_returnsCorrectInfo() {
        when(brpRepository.findMaxFechaIngesta()).thenReturn(20261117);
        when(brpRepository.findByFechaIngesta(20261117)).thenReturn(Arrays.asList(brp1, brp2));
        when(brpRepository.count()).thenReturn(2L);
        when(brpRepository.findByEstado(EstadoBrp.DISPONIBLE)).thenReturn(Arrays.asList(brp1));

        CatalogUpdateResponse result = brpService.getCatalogoUpdateInfo();

        assertThat(result.getTotalBrp()).isEqualTo(2);
        assertThat(result.getDisponibles()).isEqualTo(1);
        assertThat(result.getFechaFormateada()).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    @DisplayName("findByEstado filters by estado")
    void findByEstado_filtersByEstado() {
        when(brpRepository.findByEstado(EstadoBrp.DISPONIBLE)).thenReturn(Arrays.asList(brp1));

        List<BrpResponse> result = brpService.findByEstado(EstadoBrp.DISPONIBLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstado()).isEqualTo(EstadoBrp.DISPONIBLE);
    }
}