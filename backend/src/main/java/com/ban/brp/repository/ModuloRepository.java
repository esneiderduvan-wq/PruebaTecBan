package com.ban.brp.repository;

import com.ban.brp.entity.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    Optional<Modulo> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}