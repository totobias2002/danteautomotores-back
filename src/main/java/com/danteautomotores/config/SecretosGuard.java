package com.danteautomotores.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Impide arrancar en producción con los valores por defecto del repo (públicos) para el secreto JWT
 * y la contraseña de la base, con un secreto demasiado corto para HS256, o sin las credenciales de Cloudinary
 * (sin ellas no se pueden subir ni borrar fotos). Las credenciales de Cloudinary se interpretan como válidas si están
 * presentes y no vacías: no se consulta a Cloudinary al arrancar, para no acoplar el deploy a un tercero.
 * <p>
 * También exige, fuera del modo desarrollo, la configuración de mail y de identidad: {@code BREVO_API_KEY} (sin ella
 * {@code MailConfig} usaría {@code LogEmailSender} y los links de recuperación de contraseña quedarían escritos en el
 * log), {@code MAIL_REMITENTE_EMAIL}, {@code APP_FRONTEND_URL} (con {@code https://} y que no apunte a localhost:
 * los links de los mails se arman con ella y nunca con el Host de la request) y {@code GOOGLE_CLIENT_ID}. Igual que
 * con Cloudinary, solo se verifica que no estén vacías, no se consulta a Brevo ni a Google al arrancar.
 * <p>
 * El modo permisivo (solo avisa en el log) aplica cuando NO hay ningún perfil activo y rige el perfil por defecto de
 * Spring (desarrollo local sin configuración: {@code mvn spring-boot:run} o el IDE) o cuando todos los perfiles
 * activos son de desarrollo ({@code dev}, {@code local}, {@code test}). Cualquier otro perfil ({@code prod},
 * {@code production}, {@code railway}, {@code staging}...) o una mezcla como {@code prod,dev} aborta el arranque.
 * Un deploy que no use el Dockerfile (que ya fija {@code SPRING_PROFILES_ACTIVE=prod}) debe definir un perfil.
 * El criterio es el mismo que el de {@link DataSeeder} porque ambos usan {@link EntornoDeDesarrollo}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecretosGuard implements InitializingBean {

    static final String JWT_SECRET_POR_DEFECTO = "CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES";
    static final String DB_PASSWORD_POR_DEFECTO = "dante_dev_password";
    /** Lo mínimo que exige JJWT para firmar con HS256 (256 bits). */
    static final int JWT_SECRET_MIN_BYTES = 32;

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @Value("${app.mail.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.remitente-email:}")
    private String mailRemitenteEmail;

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank() || JWT_SECRET_POR_DEFECTO.equals(jwtSecret)) {
            fallarOAvisar("APP_JWT_SECRET falta o es el valor de ejemplo público: definí un secreto propio de al menos "
                    + JWT_SECRET_MIN_BYTES + " caracteres");
        } else if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < JWT_SECRET_MIN_BYTES) {
            fallarOAvisar("APP_JWT_SECRET tiene menos de " + JWT_SECRET_MIN_BYTES
                    + " bytes: es muy corto para firmar los tokens, definí uno de al menos " + JWT_SECRET_MIN_BYTES
                    + " caracteres");
        }
        if (DB_PASSWORD_POR_DEFECTO.equals(dbPassword)) {
            fallarOAvisar("SPRING_DATASOURCE_PASSWORD es la contraseña de desarrollo del repo: definí una contraseña propia");
        }
        if (estaEnBlanco(cloudinaryCloudName) || estaEnBlanco(cloudinaryApiKey) || estaEnBlanco(cloudinaryApiSecret)) {
            fallarOAvisar("CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET faltan: "
                    + "sin ellos no se pueden subir ni borrar fotos");
        }
        if (estaEnBlanco(brevoApiKey)) {
            fallarOAvisar("BREVO_API_KEY falta: sin ella los mails (confirmación de cuenta y recuperación de contraseña) "
                    + "no se mandan y los links quedarían escritos en el log");
        }
        if (estaEnBlanco(mailRemitenteEmail)) {
            fallarOAvisar("MAIL_REMITENTE_EMAIL falta: es la dirección desde la que salen los mails");
        }
        if (estaEnBlanco(frontendUrl)) {
            fallarOAvisar("APP_FRONTEND_URL falta: es el origen del front con el que se arman los links de los mails");
        } else if (!esUrlPublicaHttps(frontendUrl)) {
            fallarOAvisar("APP_FRONTEND_URL debe empezar con https:// y no apuntar a localhost ni a 127.0.0.1: "
                    + "los links de los mails se arman con ella");
        }
        if (estaEnBlanco(googleClientId)) {
            fallarOAvisar("GOOGLE_CLIENT_ID falta: sin él no se puede entrar con Google");
        }
    }

    private static boolean esUrlPublicaHttps(String url) {
        String minuscula = url.strip().toLowerCase(Locale.ROOT);
        return minuscula.startsWith("https://") && !minuscula.contains("localhost") && !minuscula.contains("127.0.0.1");
    }

    private static boolean estaEnBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    private void fallarOAvisar(String mensaje) {
        if (!EntornoDeDesarrollo.esDesarrollo(environment)) {
            throw new IllegalStateException(mensaje);
        }
        log.warn("{} (fuera del modo desarrollo, es decir con cualquier perfil que no sea dev/local/test, el arranque se aborta)",
                mensaje);
    }
}
