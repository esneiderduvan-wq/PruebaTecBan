package com.ban.brp.repository;

import com.ban.brp.entity.Usuario;
import com.ban.brp.entity.UsuarioModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioModuloRepository extends JpaRepository<UsuarioModulo, Long> {

    @Query("SELECT um FROM UsuarioModulo um JOIN FETCH um.modulo WHERE um.usuario.id = :usuarioId")
    List<UsuarioModulo> findByUsuarioIdWithModulos(@Param("usuarioId") Long usuarioId);

    @Query("SELECT um FROM UsuarioModulo um WHERE um.usuario.username = :username")
    List<UsuarioModulo> findByUsername(@Param("username") String username);

    boolean existsByUsuarioIdAndModuloId(Long usuarioId, Long moduloId);

    void deleteByUsuarioIdAndModuloId(Long usuarioId, Long moduloId);

    @Modifying
    @Query("DELETE FROM UsuarioModulo um WHERE um.usuario.id = :usuarioId")
    void deleteAllByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.usuarioModulos um WHERE um.modulo.nombre = :moduloNombre")
    long countUsuariosByModulo(@Param("moduloNombre") String moduloNombre);
}