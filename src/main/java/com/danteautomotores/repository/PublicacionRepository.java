package com.danteautomotores.repository;

import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long>, JpaSpecificationExecutor<Publicacion> {

    // Para las operaciones sobre las fotos (subir, reordenar, borrar): serializa los pedidos concurrentes sobre la
    // misma publicación, así el tope de 10 fotos y el "orden" no se pisan. Requiere transacción activa.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Publicacion p where p.id = :id")
    Optional<Publicacion> findByIdForUpdate(@Param("id") Long id);

    List<Publicacion> findByAgenciaIdAndEstado(Long agenciaId, EstadoPublicacion estado);
    List<Publicacion> findByEstado(EstadoPublicacion estado);
    boolean existsByAgenciaId(Long agenciaId);
}
