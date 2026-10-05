package com.danteautomotores.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ServicioExternoException;
import com.danteautomotores.mail.EmailSender;
import com.danteautomotores.mail.MensajeEmail;
import com.danteautomotores.support.EmailSenderEnMemoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificacionesServiceTest {

    private static final String FRONT = "https://www.danteautomotores.com";
    private static final String TOKEN = "tokenSecretoDe43CaracteresXXXXXXXXXXXXXXXXXX1";
    private static final String MAIL = "ana.perez@example.com";

    private EmailSenderEnMemoria sender;
    private Usuario usuario;
    private Logger loggerDelServicio;
    private ListAppender<ILoggingEvent> logCapturado;

    @BeforeEach
    void preparar() {
        sender = new EmailSenderEnMemoria();
        usuario = Usuario.builder().id(7L).nombre("Ana").email(MAIL).rol(Rol.COMPRADOR).build();
        capturarLog();
    }

    @AfterEach
    void soltarLog() {
        loggerDelServicio.detachAppender(logCapturado);
    }

    private void capturarLog() {
        loggerDelServicio = (Logger) LoggerFactory.getLogger(NotificacionesService.class);
        logCapturado = new ListAppender<>();
        logCapturado.start();
        loggerDelServicio.addAppender(logCapturado);
    }

    private NotificacionesService servicio(EmailSender emailSender, String frontendUrl, int mailsPorDia) {
        NotificacionesService servicio = new NotificacionesService(emailSender, new LimitadorDeIntentos());
        ReflectionTestUtils.setField(servicio, "frontendUrl", frontendUrl);
        ReflectionTestUtils.setField(servicio, "mailsPorDia", mailsPorDia);
        return servicio;
    }

    private NotificacionesService servicio() {
        return servicio(sender, FRONT, 250);
    }

    private String log() {
        return logCapturado.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }

    @Test
    void laConfirmacionDeMailVaAlDestinatarioConElLinkExacto() {
        servicio().enviarConfirmacionEmail(usuario, TOKEN);

        MensajeEmail mail = sender.ultimo();
        assertThat(mail.paraEmail()).isEqualTo(MAIL);
        assertThat(mail.paraNombre()).isEqualTo("Ana");
        assertThat(mail.asunto()).isEqualTo("Confirmá tu mail en Dante Automotores");
        assertThat(mail.texto()).contains(FRONT + "/confirmar-email?token=" + TOKEN).contains("Hola Ana").contains("24 horas");
        assertThat(mail.html()).contains(FRONT + "/confirmar-email?token=" + TOKEN);
    }

    @Test
    void elCambioDeContrasenaVaAlDestinatarioConElLinkExacto() {
        servicio().enviarRestablecerContrasena(usuario, TOKEN);

        MensajeEmail mail = sender.ultimo();
        assertThat(mail.paraEmail()).isEqualTo(MAIL);
        assertThat(mail.asunto()).isEqualTo("Cambiá tu contraseña");
        assertThat(mail.texto()).contains(FRONT + "/restablecer-contrasena?token=" + TOKEN)
                .contains("1 hora").contains("una sola vez").contains("Si no pediste esto, ignorá este mail");
        assertThat(mail.html()).contains(FRONT + "/restablecer-contrasena?token=" + TOKEN);
    }

    @Test
    void elAvisoDeContrasenaCambiadaNoLlevaNingunLinkDeRestablecimiento() {
        servicio().enviarContrasenaCambiada(usuario);

        MensajeEmail mail = sender.ultimo();
        assertThat(mail.paraEmail()).isEqualTo(MAIL);
        assertThat(mail.asunto()).isEqualTo("Tu contraseña fue cambiada");
        assertThat(mail.texto()).contains("Olvidé mi contraseña").doesNotContain("http").doesNotContain("token=");
        assertThat(mail.html()).doesNotContain("href").doesNotContain("token=").doesNotContain("restablecer-contrasena");
    }

    @Test
    void conBarraFinalEnLaUrlDelFrontNoSeDuplicaLaBarra() {
        servicio(sender, "  " + FRONT + "/  ", 250).enviarConfirmacionEmail(usuario, TOKEN);

        assertThat(sender.ultimo().texto()).contains(FRONT + "/confirmar-email?token=" + TOKEN)
                .doesNotContain("//confirmar-email");
    }

    @Test
    void elLinkSiempreSeArmaConLaUrlConfiguradaDelFront() {
        servicio(sender, "https://otro.example.org", 250).enviarRestablecerContrasena(usuario, TOKEN);

        assertThat(sender.ultimo().texto()).contains("https://otro.example.org/restablecer-contrasena?token=" + TOKEN)
                .doesNotContain("danteautomotores.com");
    }

    @Test
    void elHtmlEscapaUnNombreConCaracteresEspeciales() {
        usuario.setNombre("<script>alert(1)</script> & \"Ana\"");

        servicio().enviarConfirmacionEmail(usuario, TOKEN);

        String html = sender.ultimo().html();
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("&amp;").contains("&quot;Ana&quot;");
    }

    @Test
    void superadoElTopeDiarioNoSeEnviaNadaYSeLogueaUnError() {
        NotificacionesService servicio = servicio(sender, FRONT, 2);

        servicio.enviarConfirmacionEmail(usuario, TOKEN);
        servicio.enviarRestablecerContrasena(usuario, TOKEN);
        servicio.enviarContrasenaCambiada(usuario);
        servicio.enviarConfirmacionEmail(usuario, TOKEN);

        assertThat(sender.enviados()).hasSize(2);
        assertThat(logCapturado.list).filteredOn(e -> e.getLevel() == Level.ERROR).hasSize(2);
        assertThat(log()).contains("Tope diario de mails superado").doesNotContain(MAIL).doesNotContain(TOKEN);
    }

    @Test
    void unEnvioQueFallaNoHaceLanzarAlMetodoYElLogNoLlevaElTokenNiElMail() {
        EmailSender roto = mensaje -> {
            throw new ServicioExternoException("fallo con " + mensaje.paraEmail() + " " + mensaje.texto());
        };
        NotificacionesService servicio = servicio(roto, FRONT, 250);

        assertThatCode(() -> servicio.enviarConfirmacionEmail(usuario, TOKEN)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.enviarRestablecerContrasena(usuario, TOKEN)).doesNotThrowAnyException();
        assertThatCode(() -> servicio.enviarContrasenaCambiada(usuario)).doesNotThrowAnyException();

        assertThat(logCapturado.list).filteredOn(e -> e.getLevel() == Level.ERROR).hasSize(3);
        String salida = log();
        assertThat(salida).contains("confirmación de mail").contains("cambio de contraseña")
                .contains("aviso de contraseña cambiada");
        assertThat(salida).doesNotContain(TOKEN).doesNotContain(MAIL).doesNotContain("ana.perez")
                .doesNotContain("token=").doesNotContain("http");
        // Tampoco se serializa la excepción con su mensaje (traería el mail y el link).
        assertThat(logCapturado.list).allSatisfy(e -> assertThat(e.getThrowableProxy()).isNull());
    }

    @Test
    void unaFallaInesperadaDeCualquierTipoTampocoSeRelanza() {
        EmailSender roto = mensaje -> {
            throw new IllegalStateException("boom " + mensaje.paraEmail());
        };

        assertThatCode(() -> servicio(roto, FRONT, 250).enviarContrasenaCambiada(usuario)).doesNotThrowAnyException();
        assertThat(log()).doesNotContain(MAIL).contains("IllegalStateException");
    }

    @Test
    void unTokenNuloNoRompeElEnvio() {
        // El link se arma dentro del try: una entrada inválida se loguea y no sale como excepción.
        assertThatCode(() -> servicio().enviarConfirmacionEmail(null, TOKEN)).doesNotThrowAnyException();
        assertThat(sender.enviados()).isEmpty();
    }
}
