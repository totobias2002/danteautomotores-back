package com.danteautomotores.repository;

import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long>, JpaSpecificationExecutor<Publicacion> {
    List<Publicacion> findByAgenciaIdAndEstado(Long agenciaId, EstadoPublicacion estado);
    List<Publicacion> findByEstado(EstadoPublicacion estado);
}
