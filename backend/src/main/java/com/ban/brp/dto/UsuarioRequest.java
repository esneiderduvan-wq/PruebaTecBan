package com.ban.brp.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequest {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "Entre 3 y 50 caracteres")
    private String username;

    // Password solo requerido en creación, no en edición
    @Size(min = 6, max = 100, message = "Mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 255, message = "Máximo 255 caracteres")
    private String email;

    @NotNull(message = "El rol es obligatorio")
    private String rol;
}