package com.danteautomotores.repository;

import com.danteautomotores.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByPublicacionIdOrderByFechaDesc(Long publicacionId);
}
