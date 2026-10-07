package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.mail.EmailSender;
import com.danteautomotores.mail.MensajeEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Envía los mails de cuenta (confirmar el mail, cambiar la contraseña y aviso de contraseña cambiada) y los avisos de
 * mensaje nuevo (sin el texto del mensaje, uno cada 10 minutos por conversación y destinatario) por el ejecutor
 * asíncrono: la respuesta HTTP no espera a Brevo ni delata por tiempos si una cuenta existe. Ningún método lanza: una
 * falla de mail no rompe el registro ni la recuperación, que el usuario puede repetir (el reenvío es explícito).
 * El log lleva solo el tipo de mail: nunca el destinatario, el token ni el link.
 *
 * <p>Los links se arman siempre con {@code app.frontend-url}, nunca con el Host ni el Origin de una request.
 * Si la cola del ejecutor está llena, el envío se descarta y se loguea en {@code AsyncConfig} (el rechazo ocurre antes
 * de entrar a estos métodos).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionesService {

    static final String CLAVE_TOPE_DIARIO = "mails:dia";
    static final Duration VENTANA_TOPE_DIARIO = Duration.ofHours(24);

    static final Duration VENTANA_AVISO_DE_MENSAJE = Duration.ofMinutes(10);

    private final EmailSender emailSender;
    private final LimitadorDeIntentos limitador;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // El plan gratis de Brevo permite 300 mails por día en total: el tope deja margen.
    @Value("${app.seguridad.mails-por-dia:250}")
    private int mailsPorDia = 250;

    @Async("mailExecutor")
    public void enviarConfirmacionEmail(Usuario usuario, String token) {
        enviar("confirmación de mail", () -> PlantillasEmail.confirmarEmail(
                usuario.getNombre(), usuario.getEmail(), armarLink("/confirmar-email", token)));
    }

    @Async("mailExecutor")
    public void enviarRestablecerContrasena(Usuario usuario, String token) {
        enviar("cambio de contraseña", () -> PlantillasEmail.restablecerContrasena(
                usuario.getNombre(), usuario.getEmail(), armarLink("/restablecer-contrasena", token)));
    }

    @Async("mailExecutor")
    public void enviarContrasenaCambiada(Usuario usuario) {
        enviar("aviso de contraseña cambiada", () -> PlantillasEmail.contrasenaCambiada(
                usuario.getNombre(), usuario.getEmail()));
    }

    /** Avisa al usuario que la agencia le escribió. Como mucho un mail cada 10 minutos por conversación (D-09). */
    @Async("mailExecutor")
    public void enviarAvisoDeMensajeAlUsuario(String paraEmail, String paraNombre, String descripcionDelAuto,
                                              Long conversacionId) {
        enviarAvisoDeMensaje(paraEmail, conversacionId, () -> PlantillasEmail.mensajeNuevoParaUsuario(
                paraNombre, paraEmail, descripcionDelAuto, armarLinkDeConversacion("/mensajes/", conversacionId)));
    }

    /** Avisa a una cuenta admin que un usuario escribió. Como mucho un mail cada 10 minutos por conversación (D-09). */
    @Async("mailExecutor")
    public void enviarAvisoDeMensajeALaAgencia(String paraEmail, String paraNombre, String nombreDelUsuario,
                                               String descripcionDelAuto, Long conversacionId) {
        enviarAvisoDeMensaje(paraEmail, conversacionId, () -> PlantillasEmail.mensajeNuevoParaLaAgencia(
                paraNombre, paraEmail, nombreDelUsuario, descripcionDelAuto,
                armarLinkDeConversacion("/admin/mensajes/", conversacionId)));
    }

    private void enviarAvisoDeMensaje(String paraEmail, Long conversacionId, Supplier<MensajeEmail> armarMensaje) {
        try {
            // La ventana va antes que el tope diario: un aviso frenado no gasta el tope de todos los mails.
            String clave = "aviso-mensaje:" + conversacionId + ":" + paraEmail.toLowerCase(Locale.ROOT);
            if (!limitador.intentar(clave, 1, VENTANA_AVISO_DE_MENSAJE)) {
                return;
            }
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el mail de aviso de mensaje nuevo ({})", e.getClass().getSimpleName());
            return;
        }
        enviar("aviso de mensaje nuevo", armarMensaje);
    }

    private void enviar(String tipo, Supplier<MensajeEmail> armarMensaje) {
        try {
            if (!limitador.intentar(CLAVE_TOPE_DIARIO, mailsPorDia, VENTANA_TOPE_DIARIO)) {
                log.error("Tope diario de mails superado ({} por día): no se envía el mail de {}", mailsPorDia, tipo);
                return;
            }
            emailSender.enviar(armarMensaje.get());
        } catch (RuntimeException e) {
            // Solo la clase de la excepción: su mensaje podría traer datos del destinatario.
            log.error("No se pudo enviar el mail de {} ({})", tipo, e.getClass().getSimpleName());
        }
    }

    private String baseDelFront() {
        String base = frontendUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    // Link a una conversación, sin parámetros: siempre con app.frontend-url, nunca con el Host de la request.
    private String armarLinkDeConversacion(String ruta, Long conversacionId) {
        return UriComponentsBuilder.fromUriString(baseDelFront())
                .path(ruta + conversacionId)
                .build()
                .encode()
                .toUriString();
    }

    private String armarLink(String ruta, String token) {
        return UriComponentsBuilder.fromUriString(baseDelFront())
                .path(ruta)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}
