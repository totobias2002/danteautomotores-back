package com.danteautomotores.repository;

import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.enums.AutorMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** Cuántos mensajes sin leer tiene una conversación (para armar la lista con una sola consulta agrupada). */
    interface ConteoPorConversacion {
        Long getConversacionId();

        Long getCantidad();
    }

    // El hilo completo, del más viejo al más nuevo (el id crece con cada inserción).
    List<Mensaje> findByConversacionIdOrderByIdAsc(Long conversacionId);

    // El último mensaje (el de mayor id) de cada conversación, en una sola consulta para toda la lista.
    @Query("""
            select m from Mensaje m
            where m.conversacion.id in :conversacionIds
              and m.id = (select max(m2.id) from Mensaje m2 where m2.conversacion.id = m.conversacion.id)
            """)
    List<Mensaje> findUltimosPorConversaciones(@Param("conversacionIds") Collection<Long> conversacionIds);

    // Marca como leídos los mensajes de UN autor en UNA conversación que siguen sin leer (T-04-16): nunca toca los del
    // otro lado. Devuelve cuántos cambió.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Mensaje m set m.leidoEn = :ahora
            where m.conversacion.id = :conversacionId
              and m.autorTipo = :autorTipo
              and m.leidoEn is null
            """)
    int marcarLeidos(@Param("conversacionId") Long conversacionId,
                     @Param("autorTipo") AutorMensaje autorTipo,
                     @Param("ahora") LocalDateTime ahora);

    // Sin leer por conversación, solo de las pedidas y solo de un autor; las que no tienen no aparecen.
    @Query("""
            select m.conversacion.id as conversacionId, count(m) as cantidad
            from Mensaje m
            where m.conversacion.id in :conversacionIds
              and m.autorTipo = :autorTipo
              and m.leidoEn is null
            group by m.conversacion.id
            """)
    List<ConteoPorConversacion> contarNoLeidosPorConversacion(@Param("conversacionIds") Collection<Long> conversacionIds,
                                                              @Param("autorTipo") AutorMensaje autorTipo);

    // Del comprador: los mensajes de un autor sin leer en las conversaciones de ESE usuario.
    @Query("""
            select count(m) from Mensaje m
            where m.conversacion.usuario.id = :usuarioId
              and m.autorTipo = :autorTipo
              and m.leidoEn is null
            """)
    long contarNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId, @Param("autorTipo") AutorMensaje autorTipo);

    @Query("""
            select count(distinct m.conversacion.id) from Mensaje m
            where m.conversacion.usuario.id = :usuarioId
              and m.autorTipo = :autorTipo
              and m.leidoEn is null
            """)
    long contarConversacionesConNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId,
                                                  @Param("autorTipo") AutorMensaje autorTipo);

    // De la bandeja del admin: toda la agencia, sin filtrar por usuario.
    @Query("select count(m) from Mensaje m where m.autorTipo = :autorTipo and m.leidoEn is null")
    long contarNoLeidos(@Param("autorTipo") AutorMensaje autorTipo);

    @Query("select count(distinct m.conversacion.id) from Mensaje m where m.autorTipo = :autorTipo and m.leidoEn is null")
    long contarConversacionesConNoLeidos(@Param("autorTipo") AutorMensaje autorTipo);

    // Borrado del auto (D-16): los mensajes de todas las conversaciones de la publicación, antes de borrar éstas.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Mensaje m where m.conversacion.id in (select c.id from Conversacion c where c.publicacion.id = :publicacionId)")
    int deleteByPublicacionId(@Param("publicacionId") Long publicacionId);
}
