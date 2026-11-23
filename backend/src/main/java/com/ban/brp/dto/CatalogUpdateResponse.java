package com.ban.brp.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogUpdateResponse {
    private LocalDateTime ultimaActualizacion;
    private LocalDate fechaFormateada;
    private int totalBrp;
    private int disponibles;
}