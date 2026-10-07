package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.TipoConversacion;
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

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;
    private final RegistroDeMensajes registroDeMensajes;
    private final Clock clock;

    /** "Lo quiero": abre (o reutiliza) la conversación de compra del usuario por ese auto (D-03, D-04). */
    public ConversacionResumenResponse iniciarCompra(ConversacionRequest request, String email) {
        // La cuenta se exige antes de buscar el auto, con el estado actual de la base: no se revela si el auto existe
        // a quien no puede operar (D-12).
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        verificacionCuenta.exigir(usuario);

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
        Map<Long, Mensaje> ultimos = mensajeRepository
                .findUltimosPorConversaciones(conversaciones.stream().map(Conversacion::getId).toList())
                .stream()
                .collect(Collectors.toMap(m -> m.getConversacion().getId(), Function.identity()));
        return conversaciones.stream()
                .map(c -> ConversacionMapper.toResumen(c, ultimos.get(c.getId())))
                .toList();
    }

    private static String textoAutomatico(Publicacion publicacion) {
        return "Hola, me interesa este auto: " + publicacion.getMarca() + " " + publicacion.getModelo()
                + " " + publicacion.getAnio() + ".";
    }
}
