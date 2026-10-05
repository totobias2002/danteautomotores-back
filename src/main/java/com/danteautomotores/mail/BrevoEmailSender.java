package com.danteautomotores.mail;

import com.danteautomotores.exception.ServicioExternoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de producción: API REST transaccional de Brevo ({@code POST /v3/smtp/email}) por HTTPS.
 * <p>
 * Es REST y no SMTP porque Railway bloquea SMTP saliente en los planes bajos. Los timeouts (conexión 5 s, lectura
 * 10 s) se fijan en la fábrica de requests porque Spring Boot 3.3 todavía no tiene propiedades {@code spring.http.client.*}.
 * <p>
 * Ante cualquier falla (4xx, 5xx, timeout, conexión rechazada) lanza {@link ServicioExternoException} con un mensaje
 * fijo. Lo que se loguea es solo el estado HTTP o el tipo de error y el {@code messageId}: nunca el cuerpo del mail,
 * el asunto, el destinatario ni los links de un solo uso, ni la API key.
 */
@Slf4j
public class BrevoEmailSender implements EmailSender {

    static final String MENSAJE_DE_FALLA = "No pudimos enviar el mail. Intentá de nuevo en unos minutos.";
    static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(5);
    static final Duration TIMEOUT_LECTURA = Duration.ofSeconds(10);

    private final RestClient cliente;
    private final String remitenteEmail;
    private final String remitenteNombre;
    private final String responderA;

    public BrevoEmailSender(String urlBase, String apiKey, String remitenteEmail, String remitenteNombre,
                            String responderA) {
        this(urlBase, apiKey, remitenteEmail, remitenteNombre, responderA, TIMEOUT_CONEXION, TIMEOUT_LECTURA);
    }

    /** Con timeouts a medida: los tests usan una lectura corta para no demorar. */
    BrevoEmailSender(String urlBase, String apiKey, String remitenteEmail, String remitenteNombre,
                     String responderA, Duration timeoutConexion, Duration timeoutLectura) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeoutConexion).build());
        fabrica.setReadTimeout(timeoutLectura);
        this.cliente = RestClient.builder()
                .baseUrl(sinBarraFinal(urlBase))
                .requestFactory(fabrica)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.remitenteEmail = remitenteEmail;
        this.remitenteNombre = remitenteNombre;
        this.responderA = responderA;
    }

    @Override
    public void enviar(MensajeEmail mensaje) {
        Map<String, Object> cuerpo = armarCuerpo(mensaje);
        try {
            String messageId = cliente.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .exchange((pedido, respuesta) -> {
                        HttpStatusCode estado = respuesta.getStatusCode();
                        if (!estado.is2xxSuccessful()) {
                            throw new RespuestaDeBrevoException(estado);
                        }
                        return leerMessageId(respuesta);
                    });
            log.info("Mail enviado por Brevo (messageId={})", messageId);
        } catch (RespuestaDeBrevoException e) {
            if (e.estado.value() == 401) {
                log.warn("Brevo respondió 401: revisá la BREVO_API_KEY y, en Brevo, la opción "
                        + "'Block unknown IP addresses' (Railway rota las IPs de salida y Brevo las bloquea si está activa)");
            } else {
                log.warn("Brevo rechazó el envío con estado HTTP {}", e.estado.value());
            }
            throw new ServicioExternoException(MENSAJE_DE_FALLA, e);
        } catch (RestClientException e) {
            // Timeout, conexión rechazada, corte a mitad de la respuesta: solo el tipo de error, sin el mensaje.
            log.warn("No se pudo contactar a Brevo ({})", e.getClass().getSimpleName());
            throw new ServicioExternoException(MENSAJE_DE_FALLA, e);
        }
    }

    private Map<String, Object> armarCuerpo(MensajeEmail m) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sender", Map.of("name", remitenteNombre, "email", remitenteEmail));
        cuerpo.put("to", List.of(Map.of("email", m.paraEmail(), "name", m.paraNombre())));
        cuerpo.put("subject", m.asunto());
        cuerpo.put("htmlContent", m.html());
        cuerpo.put("textContent", m.texto());
        if (responderA != null && !responderA.isBlank()) {
            cuerpo.put("replyTo", Map.of("email", responderA));
        }
        return cuerpo;
    }

    /** El mail ya salió (2xx): si la respuesta viene rara solo se pierde el id del log, nunca se informa una falla. */
    private static String leerMessageId(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse respuesta) {
        try {
            Map<?, ?> cuerpo = respuesta.bodyTo(Map.class);
            Object id = cuerpo == null ? null : cuerpo.get("messageId");
            return id == null ? "desconocido" : id.toString();
        } catch (RuntimeException e) {
            return "desconocido";
        }
    }

    private static String sinBarraFinal(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Respuesta no exitosa de Brevo; el estado alcanza para el log y la causa no arrastra el cuerpo de la respuesta. */
    private static final class RespuestaDeBrevoException extends RuntimeException {
        private final transient HttpStatusCode estado;

        RespuestaDeBrevoException(HttpStatusCode estado) {
            super("Brevo respondió con estado HTTP " + estado.value());
            this.estado = estado;
        }
    }
}
