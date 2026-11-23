package com.ban.brp.repository;

import com.ban.brp.entity.EstadoOferta;
import com.ban.brp.entity.Oferta;
import com.ban.brp.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    Optional<Oferta> findByBrpCodigo(String brpCodigo);

    boolean existsByBrpCodigo(String brpCodigo);

    List<Oferta> findByEstado(EstadoOferta estado);

    List<Oferta> findByRadicadoPorId(Long radicadoPorId);

    List<Oferta> findByAnalistaId(Long analistaId);
   
    @Query("SELECT o FROM Oferta o WHERE o.radicadoPor.username = :username")
    List<Oferta> findByRadicadoPorUsername(@Param("username") String username);
    
    @Query("SELECT o FROM Oferta o WHERE o.estado = 'EN_PROCESO' OR o.analista.username = :username")
    List<Oferta> findForAnalista(@Param("username") String username);

    @Query("SELECT o FROM Oferta o WHERE o.estado = 'ESCALADO'")
    List<Oferta> findEscaladas();

    @Query("SELECT o FROM Oferta o ORDER BY o.fechaCreacion DESC")
    List<Oferta> findAllOrderByFechaCreacionDesc();

    @Query("SELECT o FROM Oferta o JOIN o.brp b WHERE b.codigo = :codigo")
    List<Oferta> findByBrpCodigoLike(@Param("codigo") String codigo);

    @Query("SELECT COUNT(o) FROM Oferta o WHERE o.estado = :estado")
    long countByEstado(@Param("estado") EstadoOferta estado);
}