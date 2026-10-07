package com.danteautomotores.repository;

import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long>, JpaSpecificationExecutor<Conversacion> {

    // La bandeja del admin: usuario, auto y agencia vienen en la misma consulta de la página (el resumen los recorre).
    @Override
    @EntityGraph(attributePaths = {"usuario", "publicacion", "publicacion.agencia"})
    Page<Conversacion> findAll(Specification<Conversacion> spec, Pageable pageable);

    Optional<Conversacion> findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
            Long usuarioId, Long publicacionId, TipoConversacion tipo, EstadoConversacion estado);

    // El auto y su agencia vienen en la misma consulta: la lista de Mis mensajes arma el resumen de cada auto.
    @EntityGraph(attributePaths = {"publicacion", "publicacion.agencia"})
    List<Conversacion> findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(Long usuarioId);

    // Toda búsqueda del lado del comprador va por id y dueño: una conversación ajena es igual a una inexistente (D-12).
    @EntityGraph(attributePaths = {"publicacion", "publicacion.agencia"})
    Optional<Conversacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    // Reabrir una compra no puede dejar dos abiertas del mismo usuario por el mismo auto (D-08): se mira cualquier otra
    // conversación abierta distinta de la que se reabre.
    boolean existsByUsuarioIdAndPublicacionIdAndTipoAndEstadoAndIdNot(
            Long usuarioId, Long publicacionId, TipoConversacion tipo, EstadoConversacion estado, Long id);
}
