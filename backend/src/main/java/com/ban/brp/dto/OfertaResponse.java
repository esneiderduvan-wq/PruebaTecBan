package com.ban.brp.dto;

import com.ban.brp.entity.EstadoOferta;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfertaResponse {
    private Long id;
    private String brpCodigo;
    private String brpDescripcion;
    private BigDecimal valorTotal;
    private BigDecimal descuento;
    private EstadoOferta estado;
    private String radicadoPor;
    private String analista;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEscalamiento;
    private LocalDateTime fechaDevolucion;
    private String motivoDevolucion;
    private LocalDateTime fechaFinalizacion;
    private List<OferenteResponse> oferentes;
    private boolean tieneAdjuntos;
    private int totalAdjuntos;
}