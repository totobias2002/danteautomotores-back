package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.mapper.ConversacionMapper;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.spec.ConversacionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lo que la agencia hace con las conversaciones de todos los usuarios. Solo se llega desde /api/admin/**. */
@Service
@Transactional
@RequiredArgsConstructor
public class ConversacionAdminService {

    static final int TAMANIO_DE_PAGINA = 20;

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;

    /**
     * La bandeja: de a 20, la de mensaje más reciente primero (D-15). {@code tipo} y {@code estado} nulos significan
     * "todos". El último mensaje y los no leídos de la página se resuelven con una consulta cada uno, no una por fila.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ConversacionResumenResponse> listar(TipoConversacion tipo, EstadoConversacion estado,
                                                              boolean soloNoLeidas, int pagina) {
        // Desempate por id: sin un orden total dos páginas consecutivas pueden repetir o saltear conversaciones.
        Pageable pageable = PageRequest.of(Math.max(pagina, 1) - 1, TAMANIO_DE_PAGINA,
                Sort.by(Sort.Order.desc("ultimoMensajeEn"), Sort.Order.desc("id")));
        Page<Conversacion> conversaciones =
                conversacionRepository.findAll(ConversacionSpecification.bandeja(tipo, estado, soloNoLeidas), pageable);

        List<Long> ids = conversaciones.getContent().stream().map(Conversacion::getId).toList();
        Map<Long, Mensaje> ultimos = Map.of();
        Map<Long, Long> noLeidos = Map.of();
        if (!ids.isEmpty()) {
            ultimos = mensajeRepository.findUltimosPorConversaciones(ids).stream()
                    .collect(Collectors.toMap(m -> m.getConversacion().getId(), Function.identity()));
            // Visto desde la agencia, "sin leer" son los mensajes del usuario.
            noLeidos = mensajeRepository.contarNoLeidosPorConversacion(ids, AutorMensaje.USUARIO).stream()
                    .collect(Collectors.toMap(MensajeRepository.ConteoPorConversacion::getConversacionId,
                            MensajeRepository.ConteoPorConversacion::getCantidad));
        }
        Map<Long, Mensaje> ultimosPorId = ultimos;
        Map<Long, Long> noLeidosPorId = noLeidos;
        return PaginaResponse.de(conversaciones.map(c -> ConversacionMapper.toResumenParaAdmin(
                c, ultimosPorId.get(c.getId()), noLeidosPorId.getOrDefault(c.getId(), 0L))));
    }
}
