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
    // misma publicación, así el tope de 10 fotos y el "orden" no se pisan. Requiere transacción activa, que debe ser
    // corta (nunca con una llamada de red adentro). Llamar antes a fijarTimeoutDeLock() para que la espera esté acotada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Publicacion p where p.id = :id")
    Optional<Publicacion> findByIdForUpdate(@Param("id") Long id);

    // Acota a 5 segundos la espera por un lock durante el resto de la transacción actual (lock_timeout de PostgreSQL,
    // con alcance local a la transacción). El hint jakarta.persistence.lock.timeout NO sirve acá: el dialecto de
    // PostgreSQL lo ignora y la espera quedaba sin límite. Si se agota, el driver lanza un error 55P03 que Spring
    // traduce a PessimisticLockingFailureException (ver GlobalExceptionHandler, responde 409).
    @Query(value = "select set_config('lock_timeout', '5000', true)", nativeQuery = true)
    String fijarTimeoutDeLock();

    List<Publicacion> findByAgenciaIdAndEstado(Long agenciaId, EstadoPublicacion estado);
    List<Publicacion> findByEstado(EstadoPublicacion estado);
    boolean existsByAgenciaId(Long agenciaId);
}
