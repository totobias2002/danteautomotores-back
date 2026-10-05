package com.danteautomotores.config;

import com.danteautomotores.mail.BrevoEmailSender;
import com.danteautomotores.mail.EmailSender;
import com.danteautomotores.mail.LogEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige la implementación de {@link EmailSender} según la configuración: con {@code BREVO_API_KEY} manda de verdad por
 * Brevo; sin ella solo escribe los mails en el log (desarrollo y tests). Fuera del modo desarrollo
 * {@link SecretosGuard} exige la clave, así que producción nunca cae en el log. La API key no se loguea nunca.
 */
@Slf4j
@Configuration
public class MailConfig {

    @Bean
    public EmailSender emailSender(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.brevo.api-url:https://api.brevo.com}") String apiUrl,
            @Value("${app.mail.remitente-email:}") String remitenteEmail,
            @Value("${app.mail.remitente-nombre:Dante Automotores}") String remitenteNombre,
            @Value("${app.mail.responder-a:}") String responderA) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO_API_KEY no está configurada: los mails NO se mandan, solo se escriben en el log "
                    + "(esto solo es válido en desarrollo)");
            return new LogEmailSender();
        }
        log.info("Mails por Brevo (remitente configurado: {})", remitenteEmail == null || remitenteEmail.isBlank() ? "no" : "sí");
        return new BrevoEmailSender(apiUrl, apiKey, remitenteEmail, remitenteNombre, responderA);
    }
}
