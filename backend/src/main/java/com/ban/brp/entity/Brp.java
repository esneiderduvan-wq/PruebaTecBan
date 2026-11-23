package com.ban.brp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "brp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brp {

    @Id
    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoBrp estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_brp", nullable = false, length = 30)
    private TipoBrp tipoBrp;

    @Column(name = "matricula_inmobiliaria", length = 50)
    private String matriculaInmobiliaria;

    @Column(name = "placa", length = 20)
    private String placa;

    @Column(name = "departamento", nullable = false, length = 100)
    private String departamento;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio", precision = 15, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_asignacion_precio")
    private LocalDate fechaAsignacionPrecio;

    @Column(name = "anio_ingesta", nullable = false)
    private Integer anioIngesta;

    @Column(name = "mes_ingesta", nullable = false)
    private Integer mesIngesta;

    @Column(name = "dia_ingesta", nullable = false)
    private Integer diaIngesta;

    @Column(name = "ultima_actualizacion", nullable = false)
    private java.time.LocalDateTime ultimaActualizacion;
}