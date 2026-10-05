package com.danteautomotores.mail;

import com.danteautomotores.exception.ServicioExternoException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contra un servidor HTTP local de la JDK: no toca la red ni la API real de Brevo. */
@ExtendWith(OutputCaptureExtension.class)
class BrevoEmailSenderTest {

    private static final String API_KEY_DE_PRUEBA = "clave-de-prueba-que-no-es-real";
    private static final String TEXTO_SECRETO = "Tu código de ñandú: http://prueba.invalid/restablecer?token=ABC123SECRETO";
    private static final MensajeEmail MENSAJE = new MensajeEmail(
            "ana@example.com", "Ana Gómez", "Restablecé tu contraseña — Dante",
            "<p>" + TEXTO_SECRETO + "</p>", TEXTO_SECRETO);

    private final ObjectMapper json = new ObjectMapper();

    private HttpServer servidor;
    private final AtomicReference<String> metodo = new AtomicReference<>();
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> apiKeyRecibida = new AtomicReference<>();
    private final AtomicReference<String> contentTypeRecibido = new AtomicReference<>();
    private final AtomicReference<byte[]> cuerpoRecibido = new AtomicReference<>();
    private volatile int estadoDeRespuesta;
    private volatile String cuerpoDeRespuesta;
    private volatile long demoraMs;

    @BeforeEach
    void levantarServidor() throws IOException {
        estadoDeRespuesta = 201;
        cuerpoDeRespuesta = "{\"messageId\":\"<abc@smtp-relay.mailin.fr>\"}";
        demoraMs = 0;
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            metodo.set(intercambio.getRequestMethod());
            ruta.set(intercambio.getRequestURI().getPath());
            apiKeyRecibida.set(intercambio.getRequestHeaders().getFirst("api-key"));
            contentTypeRecibido.set(intercambio.getRequestHeaders().getFirst("Content-Type"));
            cuerpoRecibido.set(intercambio.getRequestBody().readAllBytes());
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] respuesta = cuerpoDeRespuesta.getBytes(StandardCharsets.UTF_8);
            try {
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(estadoDeRespuesta, respuesta.length);
                intercambio.getResponseBody().write(respuesta);
            } catch (IOException ignorado) {
                // el cliente ya cortó por timeout
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    @AfterEach
    void apagarServidor() {
        servidor.stop(0);
    }

    private String urlBase() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private BrevoEmailSender sender(String responderA) {
        return new BrevoEmailSender(urlBase(), API_KEY_DE_PRUEBA, "dante@example.com", "Dante Automotores", responderA);
    }

    private JsonNode cuerpoComoJson() throws IOException {
        return json.readTree(new String(cuerpoRecibido.get(), StandardCharsets.UTF_8));
    }

    @Test
    void elExitoManda_laApiKey_elJson_yElCuerpoDelContrato() throws IOException {
        sender(null).enviar(MENSAJE);

        assertThat(metodo.get()).isEqualTo("POST");
        assertThat(ruta.get()).isEqualTo("/v3/smtp/email");
        assertThat(apiKeyRecibida.get()).isEqualTo(API_KEY_DE_PRUEBA);
        assertThat(contentTypeRecibido.get()).startsWith("application/json");
        JsonNode cuerpo = cuerpoComoJson();
        assertThat(cuerpo.at("/sender/email").asText()).isEqualTo("dante@example.com");
        assertThat(cuerpo.at("/sender/name").asText()).isEqualTo("Dante Automotores");
        assertThat(cuerpo.at("/to/0/email").asText()).isEqualTo("ana@example.com");
        assertThat(cuerpo.at("/to/0/name").asText()).isEqualTo("Ana Gómez");
        assertThat(cuerpo.get("subject").asText()).isEqualTo("Restablecé tu contraseña — Dante");
        assertThat(cuerpo.get("htmlContent").asText()).isEqualTo("<p>" + TEXTO_SECRETO + "</p>");
        assertThat(cuerpo.get("textContent").asText()).isEqualTo(TEXTO_SECRETO);
        assertThat(cuerpo.has("replyTo")).isFalse();
    }

    @Test
    void losAcentosViajanEnUtf8() {
        sender(null).enviar(MENSAJE);

        String crudo = new String(cuerpoRecibido.get(), StandardCharsets.UTF_8);
        assertThat(crudo).contains("Gómez").contains("Restablecé").contains("ñandú").contains("—");
    }

    @Test
    void elResponderASeEnviaComoReplyToSoloSiEstaConfigurado() throws IOException {
        sender("ventas@example.com").enviar(MENSAJE);
        assertThat(cuerpoComoJson().at("/replyTo/email").asText()).isEqualTo("ventas@example.com");

        sender("   ").enviar(MENSAJE);
        assertThat(cuerpoComoJson().has("replyTo")).isFalse();
    }

    @Test
    void laRespuestaSinMessageIdNoSeTomaPorFalla() {
        cuerpoDeRespuesta = "no es json";

        assertThatCode(() -> sender(null).enviar(MENSAJE)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void unaRespuestaNoExitosaLanzaServicioExternoConMensajeFijo(int estado) {
        estadoDeRespuesta = estado;
        cuerpoDeRespuesta = "{\"message\":\"detalle del proveedor\"}";

        assertThatThrownBy(() -> sender(null).enviar(MENSAJE))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessage(BrevoEmailSender.MENSAJE_DE_FALLA)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("ñandú").doesNotContain("ABC123SECRETO"));
    }

    @Test
    void un401SeLogueaAparteMencionandoLaApiKeyYLasIps(CapturedOutput salida) {
        estadoDeRespuesta = 401;

        assertThatThrownBy(() -> sender(null).enviar(MENSAJE)).isInstanceOf(ServicioExternoException.class);

        assertThat(salida.getAll()).contains("401").contains("BREVO_API_KEY").contains("Block unknown IP addresses");
    }

    @Test
    void unServidorQueNoRespondeDentroDelTimeoutLanzaServicioExterno() {
        demoraMs = 2_000;
        BrevoEmailSender lento = new BrevoEmailSender(urlBase(), API_KEY_DE_PRUEBA, "dante@example.com",
                "Dante Automotores", null, Duration.ofSeconds(1), Duration.ofMillis(300));

        assertThatThrownBy(() -> lento.enviar(MENSAJE))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessage(BrevoEmailSender.MENSAJE_DE_FALLA);
    }

    @Test
    void unaConexionRechazadaLanzaServicioExterno() {
        int puertoCerrado = servidor.getAddress().getPort();
        servidor.stop(0);
        BrevoEmailSender sinServidor = new BrevoEmailSender("http://127.0.0.1:" + puertoCerrado, API_KEY_DE_PRUEBA,
                "dante@example.com", "Dante Automotores", null);

        assertThatThrownBy(() -> sinServidor.enviar(MENSAJE))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessage(BrevoEmailSender.MENSAJE_DE_FALLA);
    }

    @Test
    void ningunLogNiLaExcepcionLlevanElTextoDelMailNiLaApiKey(CapturedOutput salida) {
        // éxito
        sender(null).enviar(MENSAJE);
        // 400, 401 y 500
        for (int estado : new int[]{400, 401, 500}) {
            estadoDeRespuesta = estado;
            assertThatThrownBy(() -> sender(null).enviar(MENSAJE))
                    .isInstanceOf(ServicioExternoException.class)
                    .satisfies(e -> {
                        assertThat(e.getMessage()).doesNotContain("ABC123SECRETO");
                        assertThat(e.getCause().getMessage()).doesNotContain("ABC123SECRETO");
                    });
        }
        // timeout
        demoraMs = 1_500;
        BrevoEmailSender lento = new BrevoEmailSender(urlBase(), API_KEY_DE_PRUEBA, "dante@example.com",
                "Dante Automotores", null, Duration.ofSeconds(1), Duration.ofMillis(200));
        assertThatThrownBy(() -> lento.enviar(MENSAJE)).isInstanceOf(ServicioExternoException.class);

        assertThat(salida.getAll())
                .contains("messageId")
                .doesNotContain("ABC123SECRETO")
                .doesNotContain("ñandú")
                .doesNotContain("Restablecé")
                .doesNotContain("ana@example.com")
                .doesNotContain(API_KEY_DE_PRUEBA);
    }
}
