package com.ban.brp.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class BrpCsvRecord {
    private String codigo;
    private String estado;
    private String tipoBrp;
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
}