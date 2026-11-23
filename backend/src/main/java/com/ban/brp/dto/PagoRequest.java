package com.ban.brp.dto;

import com.ban.brp.entity.TipoPago;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoRequest {

    @NotNull(message = "El tipo de pago es obligatorio")
    private TipoPago tipoPago;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "El valor es obligatorio")
    @Positive(message = "El valor debe ser positivo")
    private BigDecimal valor;

    @Size(max = 100, message = "Máximo 100 caracteres")
    private String entidadFinanciadora;
}