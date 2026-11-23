package com.ban.brp.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long id;
    private String username;
    private String nombre;
    private String apellido;
    private String email;
    private Boolean activo;
    private String rol;
    private LocalDateTime createdAt;
    private List<String> modulos;
}