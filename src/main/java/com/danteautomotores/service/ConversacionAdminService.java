package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.ConversacionMapper;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.repository.spec.ConversacionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final UsuarioRepository usuarioRepository;
    private final RegistroDeMensajes registroDeMensajes;
    private final Clock clock;

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

        List<ConversacionResumenResponse> filas = resumir(conversaciones.getContent());
        return PaginaResponse.de(new PageImpl<>(filas, conversaciones.getPageable(), conversaciones.getTotalElements()));
    }

    /**
     * Arma los resúmenes de la agencia de una lista de conversaciones, en el mismo orden: con el usuario, el último
     * mensaje y los no leídos (los del usuario que la agencia no leyó). Una consulta agrupada cada uno, no una por
     * fila. Lo comparten la bandeja y la ficha del usuario. Necesita una transacción abierta.
     */
    @Transactional(readOnly = true)
    public List<ConversacionResumenResponse> resumir(List<Conversacion> conversaciones) {
        List<Long> ids = conversaciones.stream().map(Conversacion::getId).toList();
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
        return conversaciones.stream()
                .map(c -> ConversacionMapper.toResumenParaAdmin(
                        c, ultimosPorId.get(c.getId()), noLeidosPorId.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /** El hilo de cualquier conversación, visto desde la agencia: con el usuario y sus mensajes sin leer. */
    @Transactional(readOnly = true)
    public ConversacionDetalleResponse obtener(Long id) {
        Conversacion conversacion = buscar(id);
        return ConversacionMapper.toDetalleParaAdmin(conversacion, mensajeRepository.findByConversacionIdOrderByIdAsc(id));
    }

    /**
     * La agencia responde. Queda registrada la cuenta admin que escribió (D-05, T-04-23) aunque el usuario lo vea como
     * "Dante Automotores". Una conversación cerrada no recibe mensajes de nadie (D-08, T-04-24).
     */
    public MensajeResponse responder(Long id, MensajeRequest request, String emailAdmin) {
        Usuario admin = usuarioRepository.findByEmailIgnoreCase(emailAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        Conversacion conversacion = buscar(id);
        if (conversacion.getEstado() == EstadoConversacion.CERRADA) {
            throw new ReglaDeNegocioException("Esta conversación está cerrada. Reabrila para responder.");
        }
        Mensaje mensaje = registroDeMensajes.agregar(conversacion, admin, AutorMensaje.AGENCIA, request.getTexto().strip());
        return ConversacionMapper.toMensaje(mensaje);
    }

    /**
     * La agencia abrió el hilo: marca como leídos los mensajes del usuario de ESA conversación (nunca los de la
     * agencia) y devuelve lo que le queda sin leer en toda la bandeja (D-06).
     */
    public NoLeidosResponse marcarLeida(Long id) {
        Conversacion conversacion = buscar(id);
        mensajeRepository.marcarLeidos(conversacion.getId(), AutorMensaje.USUARIO, ahoraUtc());
        return NoLeidosResponse.builder()
                .noLeidos(mensajeRepository.contarNoLeidos(AutorMensaje.USUARIO))
                .conversaciones(mensajeRepository.contarConversacionesConNoLeidos(AutorMensaje.USUARIO))
                .build();
    }

    /** Cierra una conversación abierta (D-08). Si ya estaba cerrada no hace nada. No manda mail. */
    public ConversacionResumenResponse cerrar(Long id) {
        Conversacion conversacion = buscar(id);
        if (conversacion.getEstado() == EstadoConversacion.ABIERTA) {
            conversacion.setEstado(EstadoConversacion.CERRADA);
            conversacion.setCerradaEn(ahoraUtc());
        }
        return resumenParaAdmin(conversacion);
    }

    /**
     * Reabre una conversación cerrada (D-08). Una compra no se reabre si el usuario ya tiene otra abierta por el mismo
     * auto: el índice único parcial de la base es la última defensa, esto da el mensaje claro antes. No manda mail.
     */
    public ConversacionResumenResponse reabrir(Long id) {
        Conversacion conversacion = buscar(id);
        if (conversacion.getEstado() == EstadoConversacion.CERRADA) {
            if (conversacion.getTipo() == TipoConversacion.COMPRA && conversacion.getPublicacion() != null
                    && conversacionRepository.existsByUsuarioIdAndPublicacionIdAndTipoAndEstadoAndIdNot(
                            conversacion.getUsuario().getId(), conversacion.getPublicacion().getId(),
                            TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, conversacion.getId())) {
                throw new ReglaDeNegocioException("El usuario ya tiene otra conversación abierta por este auto.");
            }
            conversacion.setEstado(EstadoConversacion.ABIERTA);
            conversacion.setCerradaEn(null);
            // Si el comprador la había borrado de su lista, al reabrirla vuelve a verla.
            conversacion.setOcultaParaUsuario(false);
        }
        return resumenParaAdmin(conversacion);
    }

    private Conversacion buscar(Long id) {
        return conversacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la conversación"));
    }

    private ConversacionResumenResponse resumenParaAdmin(Conversacion conversacion) {
        List<Long> ids = List.of(conversacion.getId());
        Mensaje ultimo = mensajeRepository.findUltimosPorConversaciones(ids).stream().findFirst().orElse(null);
        long noLeidos = mensajeRepository.contarNoLeidosPorConversacion(ids, AutorMensaje.USUARIO).stream()
                .mapToLong(MensajeRepository.ConteoPorConversacion::getCantidad).sum();
        return ConversacionMapper.toResumenParaAdmin(conversacion, ultimo, noLeidos);
    }

    // UTC explícito (D-13): el reloj del proyecto usa la zona del servidor.
    private LocalDateTime ahoraUtc() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }
}
