package com.ban.brp.exception;

import com.ban.brp.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleValidationExceptions returns 400 Bad Request")
    void handleValidationExceptions_returns400() {
        // Arrange
        FieldError fieldError = new FieldError("obj", "username", "El username es obligatorio");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(fieldError);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        when(request.getRequestURI()).thenReturn("/api/usuarios");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getFieldErrors()).hasSize(1);
        assertThat(response.getBody().getFieldErrors().get(0).getField()).isEqualTo("username");
    }

    @Test
    @DisplayName("handleBadCredentials returns 401 Unauthorized")
    void handleBadCredentials_returns401() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("Credenciales inválidas");
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Bad Credentials");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 Forbidden")
    void handleAccessDenied_returns403() {
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("No tiene permisos");
        when(request.getRequestURI()).thenReturn("/api/usuarios");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("handleResourceNotFound returns 404 Not Found")
    void handleResourceNotFound_returns404() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Usuario no encontrado");
        when(request.getRequestURI()).thenReturn("/api/usuarios/999");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Usuario no encontrado");
    }

    @Test
    @DisplayName("handleBusinessException returns 409 Conflict")
    void handleBusinessException_returns409() {
        // Arrange
        BusinessException ex = new BusinessException("La oferta ya tiene 5 oferentes");
        when(request.getRequestURI()).thenReturn("/api/ofertas");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Business Rule Violation");
    }

    @Test
    @DisplayName("handleOptimisticLock returns 409 Conflict")
    void handleOptimisticLock_returns409() {
        // Arrange
        OptimisticLockException ex = new OptimisticLockException("Recurso modificado");
        when(request.getRequestURI()).thenReturn("/api/ofertas/1");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOptimisticLock(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("handleGenericException returns 500 Internal Server Error")
    void handleGenericException_returns500() {
        // Arrange
        Exception ex = new RuntimeException("Error inesperado");
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
    }
}