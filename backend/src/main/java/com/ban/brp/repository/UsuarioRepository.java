package com.ban.brp.repository;

import com.ban.brp.entity.Usuario;
import com.ban.brp.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Usuario> findByRol(Rol rol);

    List<Usuario> findByActivoTrue();

    @Query("SELECT u FROM Usuario u WHERE u.rol = 'ANALISTA' AND u.activo = true")
    List<Usuario> findAnalistas();

    @Query("SELECT u FROM Usuario u WHERE u.username = :username AND u.activo = true")
    Optional<Usuario> findActiveByUsername(@Param("username") String username);
}