package com.danteautomotores.repository;

import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
            Long usuarioId, Long publicacionId, TipoConversacion tipo, EstadoConversacion estado);

    // El auto y su agencia vienen en la misma consulta: la lista de Mis mensajes arma el resumen de cada auto.
    @EntityGraph(attributePaths = {"publicacion", "publicacion.agencia"})
    List<Conversacion> findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(Long usuarioId);
}
