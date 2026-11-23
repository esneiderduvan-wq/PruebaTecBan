package com.ban.brp.repository;

import com.ban.brp.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByOferenteId(Long oferenteId);

    @Query("SELECT SUM(p.valor) FROM Pago p WHERE p.oferente.id = :oferenteId")
    BigDecimal sumValorByOferenteId(@Param("oferenteId") Long oferenteId);

    @Query("SELECT COUNT(p) FROM Pago p WHERE p.oferente.id = :oferenteId")
    long countByOferenteId(@Param("oferenteId") Long oferenteId);
}