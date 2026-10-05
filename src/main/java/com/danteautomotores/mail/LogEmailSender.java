package com.danteautomotores.mail;

import lombok.extern.slf4j.Slf4j;

/**
 * Envío de desarrollo y tests: no manda nada y escribe el mail en el log, en una sola línea (destinatario, asunto y
 * texto plano completo, que incluye el link), para poder probar a mano el flujo de confirmación y de recuperación sin
 * una cuenta de Brevo y para correlacionar destinatario y link.
 * <p>
 * <b>Jamás debe estar activo en producción</b>: el link de recuperación quedaría escrito en el log.
 * {@code SecretosGuard} lo garantiza: fuera del modo desarrollo el back no arranca sin {@code BREVO_API_KEY}, que es
 * lo que hace que {@code MailConfig} elija {@link BrevoEmailSender}.
 */
@Slf4j
public class LogEmailSender implements EmailSender {

    @Override
    public void enviar(MensajeEmail mensaje) {
        String texto = mensaje.texto() == null ? "" : mensaje.texto().strip().replaceAll("\\s*\\R\\s*", " | ");
        log.info("[MAIL SOLO LOG, no se envió] para={} asunto={} texto={}", mensaje.paraEmail(), mensaje.asunto(), texto);
    }
}
