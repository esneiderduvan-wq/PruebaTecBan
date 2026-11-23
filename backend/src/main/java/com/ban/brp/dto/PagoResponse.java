package com.ban.brp.dto;

import com.ban.brp.entity.TipoPago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoResponse {
    private Long id;
    private TipoPago tipoPago;
    private LocalDate fecha;
    private BigDecimal valor;
    private String entidadFinanciadora;
}