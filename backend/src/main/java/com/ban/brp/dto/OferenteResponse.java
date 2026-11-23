package com.ban.brp.dto;

import com.ban.brp.entity.TipoIdentificacion;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OferenteResponse {
    private Long id;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private String nombre;
    private BigDecimal porcentajeParticipacion;
    private String email;
    private String telefono;
    private List<PagoResponse> pagos;
}