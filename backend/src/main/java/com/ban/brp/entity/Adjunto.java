package com.ban.brp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "adjunto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_id", nullable = false)
    @JsonIgnore
    private Oferta oferta;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_local", nullable = false, length = 500)
    private String rutaLocal;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "fecha_carga", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCarga = LocalDateTime.now();
}