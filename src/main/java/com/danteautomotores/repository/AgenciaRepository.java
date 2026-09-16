package com.danteautomotores.repository;

import com.danteautomotores.entity.Agencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgenciaRepository extends JpaRepository<Agencia, Long> {
    Optional<Agencia> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
