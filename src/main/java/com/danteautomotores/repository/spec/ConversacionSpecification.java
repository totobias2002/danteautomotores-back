package com.danteautomotores.repository.spec;

import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de la bandeja del admin. Cada filtro es un enum o un booleano y viaja como parámetro enlazado (Criteria
 * API): nunca se concatena SQL. Un filtro ausente no agrega ningún predicado (T-04-20).
 */
public class ConversacionSpecification {

    private ConversacionSpecification() {
    }

    /**
     * AND entre los filtros presentes. {@code soloNoLeidas} deja las conversaciones con al menos un mensaje del
     * usuario que la agencia no leyó; es un EXISTS, así que una conversación con varios sin leer sale una sola vez y
     * la consulta de conteo de la página da el mismo total.
     */
    public static Specification<Conversacion> bandeja(TipoConversacion tipo, EstadoConversacion estado, boolean soloNoLeidas) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipo"), tipo));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (soloNoLeidas) {
                Subquery<Long> sinLeer = query.subquery(Long.class);
                Root<Mensaje> mensaje = sinLeer.from(Mensaje.class);
                sinLeer.select(mensaje.<Long>get("id")).where(
                        cb.equal(mensaje.get("conversacion"), root),
                        cb.equal(mensaje.get("autorTipo"), AutorMensaje.USUARIO),
                        cb.isNull(mensaje.get("leidoEn")));
                predicados.add(cb.exists(sinLeer));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
