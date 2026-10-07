package com.danteautomotores.repository;

import com.danteautomotores.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // El hilo completo, del más viejo al más nuevo (el id crece con cada inserción).
    List<Mensaje> findByConversacionIdOrderByIdAsc(Long conversacionId);

    // El último mensaje (el de mayor id) de cada conversación, en una sola consulta para toda la lista.
    @Query("""
            select m from Mensaje m
            where m.conversacion.id in :conversacionIds
              and m.id = (select max(m2.id) from Mensaje m2 where m2.conversacion.id = m.conversacion.id)
            """)
    List<Mensaje> findUltimosPorConversaciones(@Param("conversacionIds") Collection<Long> conversacionIds);
}
