package com.ban.brp.repository;

import com.ban.brp.entity.Brp;
import com.ban.brp.entity.EstadoBrp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BrpRepository extends JpaRepository<Brp, String> {

    List<Brp> findByEstado(EstadoBrp estado);

    List<Brp> findByTipoBrp(String tipoBrp);

    @Query("SELECT b FROM Brp b WHERE b.estado = 'DISPONIBLE'")
    List<Brp> findAllDisponibles();

    @Query("SELECT MAX(b.anioIngesta * 10000 + b.mesIngesta * 100 + b.diaIngesta) FROM Brp b")
    Integer findMaxFechaIngesta();

    @Query("SELECT b FROM Brp b WHERE b.anioIngesta * 10000 + b.mesIngesta * 100 + b.diaIngesta = :fecha")
    List<Brp> findByFechaIngesta(@Param("fecha") Integer fecha);

    Optional<Brp> findByCodigo(String codigo);

    @Query("SELECT DISTINCT b.estado FROM Brp b")
    List<EstadoBrp> findDistinctEstados();

    @Query("SELECT b FROM Brp b WHERE b.codigo = :codigo ORDER BY b.anioIngesta DESC, b.mesIngesta DESC, b.diaIngesta DESC")
    List<Brp> findByCodigoOrderByFechaIngestaDesc(@Param("codigo") String codigo);
}