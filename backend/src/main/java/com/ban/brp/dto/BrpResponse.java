package com.ban.brp.dto;

import com.ban.brp.entity.EstadoBrp;
import com.ban.brp.entity.TipoBrp;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrpResponse {
    private String codigo;
    private EstadoBrp estado;
    private TipoBrp tipoBrp;
    private String matriculaInmobiliaria;
    private String placa;
    private String departamento;
    private String municipio;
    private String descripcion;
    private BigDecimal precio;
    private LocalDate fechaAsignacionPrecio;
    private Integer anioIngesta;
    private Integer mesIngesta;
    private Integer diaIngesta;
    private LocalDateTime ultimaActualizacion;
}