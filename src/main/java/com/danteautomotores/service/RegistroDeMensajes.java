package com.danteautomotores.service;

import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Único lugar donde se registra un mensaje: los avisos por mail se enganchan acá y no en cada servicio. Exige una
 * transacción abierta, así el mensaje y el movimiento de la conversación se guardan juntos o no se guardan.
 *
 * <p>El aviso al otro lado sale después del commit (si la transacción se revierte no sale), con valores simples
 * leídos dentro de la transacción y por el ejecutor asíncrono de mails. Una falla al encolarlo nunca rompe el mensaje
 * (D-09). El mail no lleva el texto del mensaje (D-11).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistroDeMensajes {

    private static final String SIN_AUTO = "una cotización";

    private final MensajeRepository mensajeRepository;
    private final Clock clock;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionesService notificaciones;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mensaje agregar(Conversacion conversacion, Usuario autor, AutorMensaje autorTipo, String texto) {
        // UTC explícito (D-13): el reloj del proyecto usa la zona del servidor.
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        Mensaje mensaje = mensajeRepository.save(Mensaje.builder()
                .conversacion(conversacion)
                .autor(autor)
                .autorTipo(autorTipo)
                .texto(texto)
                .creadoEn(ahora)
                .build());
        conversacion.setUltimoMensajeEn(ahora);
        avisarAlOtroLado(conversacion, autorTipo);
        return mensaje;
    }

    // Junta todo lo que el aviso necesita como valores simples (nada se lee de entidades perezosas después del commit)
    // y deja el envío para cuando la transacción confirma. Cualquier falla se traga: el mensaje ya está guardado.
    private void avisarAlOtroLado(Conversacion conversacion, AutorMensaje autorTipo) {
        try {
            Runnable aviso = armarAviso(conversacion, autorTipo);
            if (aviso == null) {
                return;
            }
            ContrasenaCuenta.despuesDelCommit(() -> {
                try {
                    aviso.run();
                } catch (RuntimeException e) {
                    log.error("No se pudo encolar el aviso de mensaje nuevo ({})", e.getClass().getSimpleName());
                }
            });
        } catch (RuntimeException e) {
            log.error("No se pudo preparar el aviso de mensaje nuevo ({})", e.getClass().getSimpleName());
        }
    }

    private Runnable armarAviso(Conversacion conversacion, AutorMensaje autorTipo) {
        Long conversacionId = conversacion.getId();
        String auto = describirElAuto(conversacion.getPublicacion());
        Usuario dueno = conversacion.getUsuario();

        if (autorTipo == AutorMensaje.USUARIO) {
            String nombreDelUsuario = nombreCompleto(dueno);
            List<Usuario> admins = usuarioRepository.findByRol(Rol.ADMIN);
            if (admins == null || admins.isEmpty()) {
                return null;
            }
            List<String[]> destinatarios = admins.stream()
                    .map(a -> new String[]{a.getEmail(), a.getNombre()})
                    .toList();
            return () -> destinatarios.forEach(d -> encolar(() -> notificaciones.enviarAvisoDeMensajeALaAgencia(
                    d[0], d[1], nombreDelUsuario, auto, conversacionId)));
        }

        String email = dueno.getEmail();
        String nombre = dueno.getNombre();
        return () -> encolar(() -> notificaciones.enviarAvisoDeMensajeAlUsuario(email, nombre, auto, conversacionId));
    }

    // Un destinatario que falla al encolar no impide avisar a los demás.
    private void encolar(Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.error("No se pudo encolar el aviso de mensaje nuevo ({})", e.getClass().getSimpleName());
        }
    }

    private static String describirElAuto(Publicacion publicacion) {
        if (publicacion == null) {
            return SIN_AUTO;
        }
        String descripcion = (nullAVacio(publicacion.getMarca()) + " " + nullAVacio(publicacion.getModelo()) + " "
                + (publicacion.getAnio() == null ? "" : publicacion.getAnio())).trim().replaceAll("\s+", " ");
        return descripcion.isEmpty() ? SIN_AUTO : descripcion;
    }

    private static String nombreCompleto(Usuario usuario) {
        return (nullAVacio(usuario.getNombre()) + " " + nullAVacio(usuario.getApellido())).trim();
    }

    private static String nullAVacio(String valor) {
        return valor == null ? "" : valor;
    }
}
