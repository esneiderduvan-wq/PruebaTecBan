package com.ban.brp.dto;

import com.ban.brp.entity.TipoIdentificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OferenteRequest {

    @NotNull(message = "El tipo de identificación es obligatorio")
    private TipoIdentificacion tipoIdentificacion;

    @NotBlank(message = "El número de identificación es obligatorio")
    @Size(max = 20, message = "Máximo 20 caracteres")
    private String numeroIdentificacion;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "Máximo 255 caracteres")
    private String nombre;

    @Email(message = "Email inválido")
    @Size(max = 255, message = "Máximo 255 caracteres")
    private String email;

    @Size(max = 20, message = "Máximo 20 caracteres")
    private String telefono;

    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede exceder 100")
    @Builder.Default
    private BigDecimal porcentajeParticipacion = BigDecimal.valueOf(100.00);

    @NotEmpty(message = "Debe tener al menos un pago")
    @Size(max = 6, message = "Máximo 6 pagos")
    @Builder.Default
    private List<@Valid PagoRequest> pagos = java.util.Collections.emptyList();
}