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
import java.util.function.Supplier;

/**
 * Envía los mails de cuenta (confirmar el mail, cambiar la contraseña y aviso de contraseña cambiada) por el ejecutor
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

    private String armarLink(String ruta, String token) {
        String base = frontendUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return UriComponentsBuilder.fromUriString(base)
                .path(ruta)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}
