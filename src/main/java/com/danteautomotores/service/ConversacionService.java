package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.ConversacionMapper;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ConversacionService {

    // D-10: tope de mensajes por cuenta (contando los "Lo quiero") dentro de la ventana.
    static final int MAXIMO_DE_ENVIOS = 20;
    static final Duration VENTANA_DE_ENVIOS = Duration.ofMinutes(10);

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;
    private final RegistroDeMensajes registroDeMensajes;
    private final LimitadorDeIntentos limitador;
    private final Clock clock;

    /** "Lo quiero": abre (o reutiliza) la conversación de compra del usuario por ese auto (D-03, D-04). */
    public ConversacionResumenResponse iniciarCompra(ConversacionRequest request, String email) {
        // La cuenta se exige antes de buscar el auto, con el estado actual de la base: no se revela si el auto existe
        // a quien no puede operar (D-12).
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        verificacionCuenta.exigir(usuario);
        limitarEnvios(usuario);

        Publicacion publicacion = publicacionRepository.findById(request.getPublicacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + request.getPublicacionId()));

        // Un RESERVADO sí se acepta: si la reserva se cae, el auto vuelve a estar disponible (D-04).
        if (publicacion.getEstado() == EstadoPublicacion.VENDIDO) {
            throw new ReglaDeNegocioException("Este auto ya se vendió");
        }

        String propio = request.getMensaje() == null ? "" : request.getMensaje().strip();
        Conversacion existente = conversacionRepository
                .findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
                        usuario.getId(), publicacion.getId(), TipoConversacion.COMPRA, EstadoConversacion.ABIERTA)
                .orElse(null);

        Conversacion conversacion;
        Mensaje ultimo;
        if (existente != null) {
            // Repetir "Lo quiero" devuelve la misma conversación; solo suma un mensaje si el usuario escribió uno (D-03).
            conversacion = existente;
            ultimo = propio.isEmpty()
                    ? mensajeRepository.findUltimosPorConversaciones(List.of(conversacion.getId())).stream().findFirst().orElse(null)
                    : registroDeMensajes.agregar(conversacion, usuario, AutorMensaje.USUARIO, propio);
        } else {
            LocalDateTime ahora = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
            conversacion = conversacionRepository.save(Conversacion.builder()
                    .tipo(TipoConversacion.COMPRA)
                    .estado(EstadoConversacion.ABIERTA)
                    .usuario(usuario)
                    .publicacion(publicacion)
                    .creadaEn(ahora)
                    .ultimoMensajeEn(ahora)
                    .build());
            String texto = propio.isEmpty() ? textoAutomatico(publicacion) : propio;
            ultimo = registroDeMensajes.agregar(conversacion, usuario, AutorMensaje.USUARIO, texto);
        }
        return ConversacionMapper.toResumen(conversacion, ultimo);
    }

    /** Las conversaciones del usuario autenticado, las más recientes primero. */
    @Transactional(readOnly = true)
    public List<ConversacionResumenResponse> listarMias(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        List<Conversacion> conversaciones =
                conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(usuario.getId());
        if (conversaciones.isEmpty()) {
            return List.of();
        }
        // Una sola consulta para el último mensaje de todas, en vez de una por fila.
        List<Long> ids = conversaciones.stream().map(Conversacion::getId).toList();
        Map<Long, Mensaje> ultimos = mensajeRepository
                .findUltimosPorConversaciones(ids)
                .stream()
                .collect(Collectors.toMap(m -> m.getConversacion().getId(), Function.identity()));
        // Y una sola más para los no leídos de todas (los mensajes de la agencia, visto desde el comprador).
        Map<Long, Long> noLeidos = mensajeRepository.contarNoLeidosPorConversacion(ids, AutorMensaje.AGENCIA)
                .stream()
                .collect(Collectors.toMap(MensajeRepository.ConteoPorConversacion::getConversacionId,
                        MensajeRepository.ConteoPorConversacion::getCantidad));
        return conversaciones.stream()
                .map(c -> ConversacionMapper.toResumen(c, ultimos.get(c.getId()), noLeidos.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /**
     * El comprador abrió el hilo: marca como leídos los mensajes de la agencia de ESA conversación (nunca los suyos).
     * Es una lectura, así que no exige cuenta verificada. Ajena e inexistente dan el mismo 404 (D-12, T-04-16).
     */
    public NoLeidosResponse marcarLeida(Long id, String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        Conversacion conversacion = buscarPropia(id, usuario);
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        mensajeRepository.marcarLeidos(conversacion.getId(), AutorMensaje.AGENCIA, ahora);
        return contarDelComprador(usuario);
    }

    /** Cuántos mensajes sin leer tiene quien pregunta: el admin los de los usuarios en toda la bandeja, el comprador los de la agencia (D-06). */
    @Transactional(readOnly = true)
    public NoLeidosResponse contarNoLeidos(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        if (usuario.getRol() == Rol.ADMIN) {
            return NoLeidosResponse.builder()
                    .noLeidos(mensajeRepository.contarNoLeidos(AutorMensaje.USUARIO))
                    .conversaciones(mensajeRepository.contarConversacionesConNoLeidos(AutorMensaje.USUARIO))
                    .build();
        }
        return contarDelComprador(usuario);
    }

    private NoLeidosResponse contarDelComprador(Usuario usuario) {
        return NoLeidosResponse.builder()
                .noLeidos(mensajeRepository.contarNoLeidosDeUsuario(usuario.getId(), AutorMensaje.AGENCIA))
                .conversaciones(mensajeRepository.contarConversacionesConNoLeidosDeUsuario(usuario.getId(), AutorMensaje.AGENCIA))
                .build();
    }

    /** El hilo de una conversación propia, con los mensajes en orden. Ajena e inexistente dan el mismo 404 (D-12). */
    @Transactional(readOnly = true)
    public ConversacionDetalleResponse obtenerMia(Long id, String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        Conversacion conversacion = buscarPropia(id, usuario);
        return ConversacionMapper.toDetalle(conversacion, mensajeRepository.findByConversacionIdOrderByIdAsc(conversacion.getId()));
    }

    /** El comprador escribe en su conversación. Exige cuenta completa, respeta el límite y rechaza las cerradas. */
    public MensajeResponse enviarMensaje(Long id, MensajeRequest request, String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        verificacionCuenta.exigir(usuario);
        limitarEnvios(usuario);

        Conversacion conversacion = buscarPropia(id, usuario);
        if (conversacion.getEstado() == EstadoConversacion.CERRADA) {
            throw new ReglaDeNegocioException("Esta conversación está cerrada.");
        }
        Mensaje mensaje = registroDeMensajes.agregar(conversacion, usuario, AutorMensaje.USUARIO, request.getTexto().strip());
        return ConversacionMapper.toMensaje(mensaje);
    }

    private Conversacion buscarPropia(Long id, Usuario usuario) {
        return conversacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la conversación"));
    }

    // D-10: 20 mensajes cada 10 minutos por cuenta; el "Lo quiero" también cuenta (T-04-04).
    private void limitarEnvios(Usuario usuario) {
        if (!limitador.intentar("msg:" + usuario.getId(), MAXIMO_DE_ENVIOS, VENTANA_DE_ENVIOS)) {
            throw new LimiteDeIntentosException(
                    "Enviaste muchos mensajes seguidos. Esperá unos minutos y volvé a intentar.", VENTANA_DE_ENVIOS.toSeconds());
        }
    }

    private static String textoAutomatico(Publicacion publicacion) {
        return "Hola, me interesa este auto: " + publicacion.getMarca() + " " + publicacion.getModelo()
                + " " + publicacion.getAnio() + ".";
    }
}
