package com.ban.brp.security;

import com.ban.brp.entity.Rol;
import com.ban.brp.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", 
            "brp-secret-key-para-jwt-debe-ser-muy-largo-mínimo-256-bits-para-funcionar-bien");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 28800000L);

        usuario = Usuario.builder()
                .id(1L)
                .username("admin")
                .password("encodedPassword")
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@test.com")
                .rol(Rol.ADMINISTRADOR)
                .activo(true)
                .build();

        userDetails = new CustomUserDetails(usuario, Collections.emptyList());
    }

    @Test
    @DisplayName("generateToken creates valid JWT token")
    void generateToken_createsValidToken() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("extractUsername returns correct username from token")
    void extractUsername_returnsCorrectUsername() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("admin");
    }

    @Test
    @DisplayName("isTokenValid returns true for valid token")
    void isTokenValid_returnsTrueForValidToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for different user")
    void isTokenValid_returnsFalseForDifferentUser() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        
        Usuario anotherUser = Usuario.builder()
                .id(2L)
                .username("other")
                .password("encodedPassword")
                .nombre("Otro")
                .apellido("Usuario")
                .email("otro@test.com")
                .rol(Rol.RADICADOR)
                .activo(true)
                .build();
        CustomUserDetails otherDetails = new CustomUserDetails(anotherUser, Collections.emptyList());

        // Act
        boolean isValid = jwtService.isTokenValid(token, otherDetails);

        // Assert
        assertThat(isValid).isFalse();
    }
}