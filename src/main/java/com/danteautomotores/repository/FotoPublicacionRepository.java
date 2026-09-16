package com.danteautomotores.repository;

import com.danteautomotores.entity.FotoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoPublicacionRepository extends JpaRepository<FotoPublicacion, Long> {
    List<FotoPublicacion> findByPublicacionIdOrderByOrdenAsc(Long publicacionId);
}
