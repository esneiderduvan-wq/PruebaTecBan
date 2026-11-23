package com.ban.brp.repository;

import com.ban.brp.entity.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByOfertaId(Long ofertaId);

    void deleteByOfertaId(Long ofertaId);

    long countByOfertaId(Long ofertaId);
}