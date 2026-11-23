package com.ban.brp.service;

import com.ban.brp.dto.LoginRequest;
import com.ban.brp.dto.LoginResponse;
import com.ban.brp.entity.Usuario;
import com.ban.brp.exception.BusinessException;
import com.ban.brp.repository.UsuarioModuloRepository;
import com.ban.brp.repository.UsuarioRepository;
import com.ban.brp.security.CustomUserDetails;
import com.ban.brp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioModuloRepository usuarioModuloRepository;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String token = jwtService.generateToken(userDetails);

            CustomUserDetails customUser = (CustomUserDetails) userDetails;

            List<String> modulos = usuarioModuloRepository.findByUsername(customUser.getUsername()).stream()
                    .map(um -> um.getModulo().getNombre())
                    .collect(Collectors.toList());

            LoginResponse.UsuarioInfo usuarioInfo = LoginResponse.UsuarioInfo.builder()
                    .id(customUser.getId())
                    .username(customUser.getUsername())
                    .nombre(customUser.getUsername()) // Usuario entity doesn't have separate nombre/apellido in CustomUserDetails
                    .apellido("")
                    .email("")
                    .rol(customUser.getRol()) // This will be converted to string by Jackson
                    .modulos(modulos)
                    .build();

            return LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration / 1000) // Convert to seconds
                    .usuario(usuarioInfo)
                    .build();

        } catch (AuthenticationException e) {
            log.warn("Intento de login fallido para usuario: {}", request.getUsername());
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }
    }

    }