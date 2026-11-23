package com.ban.brp.repository;

import com.ban.brp.entity.Oferente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OferenteRepository extends JpaRepository<Oferente, Long> {

    List<Oferente> findByOfertaId(Long ofertaId);

    @Query("SELECT COUNT(o) FROM Oferente o WHERE o.oferta.id = :ofertaId")
    long countByOfertaId(@Param("ofertaId") Long ofertaId);

    boolean existsByOfertaIdAndNumeroIdentificacion(Long ofertaId, String numeroIdentificacion);
}