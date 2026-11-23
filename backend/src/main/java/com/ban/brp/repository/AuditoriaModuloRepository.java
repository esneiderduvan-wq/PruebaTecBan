package com.ban.brp.repository;

import com.ban.brp.entity.AuditoriaModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaModuloRepository extends JpaRepository<AuditoriaModulo, Long> {

    List<AuditoriaModulo> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<AuditoriaModulo> findByAsignadoPorOrderByFechaDesc(Long asignadoPor);
}