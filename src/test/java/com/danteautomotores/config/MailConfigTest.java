package com.danteautomotores.config;

import com.danteautomotores.mail.BrevoEmailSender;
import com.danteautomotores.mail.EmailSender;
import com.danteautomotores.mail.LogEmailSender;
import com.danteautomotores.mail.MensajeEmail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
class MailConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(MailConfig.class);

    @Test
    void sinApiKeyElBeanEsElDeLog() {
        contexto.run(ctx -> assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class));
    }

    @Test
    void conApiKeyEnBlancoElBeanEsElDeLog() {
        contexto.withPropertyValues("app.mail.brevo.api-key=   ")
                .run(ctx -> assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(LogEmailSender.class));
    }

    @Test
    void conApiKeyElBeanEsElDeBrevo() {
        contexto.withPropertyValues(
                        "app.mail.brevo.api-key=clave-de-prueba",
                        "app.mail.remitente-email=dante@example.com",
                        "app.mail.remitente-nombre=Dante Automotores")
                .run(ctx -> assertThat(ctx.getBean(EmailSender.class)).isInstanceOf(BrevoEmailSender.class));
    }

    @Test
    void laApiKeyNoSeLoguea(CapturedOutput salida) {
        contexto.withPropertyValues("app.mail.brevo.api-key=clave-de-prueba-secreta")
                .run(ctx -> assertThat(ctx).hasSingleBean(EmailSender.class));

        assertThat(salida.getAll()).doesNotContain("clave-de-prueba-secreta");
    }

    @Test
    void sinApiKeyAvisaQueLosMailsNoSeMandan(CapturedOutput salida) {
        contexto.run(ctx -> assertThat(ctx).hasSingleBean(EmailSender.class));

        assertThat(salida.getAll()).contains("BREVO_API_KEY").contains("NO se mandan");
    }

    @Test
    void unLogEmailSenderNoLanzaNiTocaLaRed() {
        LogEmailSender log = new LogEmailSender();

        assertThatCode(() -> log.enviar(new MensajeEmail(
                "ana@example.com", "Ana", "Asunto", "<p>hola</p>", "hola\nlink: http://localhost:5173/x?t=1")))
                .doesNotThrowAnyException();
    }

    @Test
    void elLogEmailSenderEscribeUnaSolaLineaConElLink(CapturedOutput salida) {
        new LogEmailSender().enviar(new MensajeEmail(
                "ana@example.com", "Ana", "Confirmá tu mail", "<p>x</p>", "Hola Ana\nEntrá a http://localhost:5173/confirmar?t=ABC\nGracias"));

        String linea = salida.getAll().lines()
                .filter(l -> l.contains("MAIL SOLO LOG"))
                .findFirst().orElseThrow();
        assertThat(linea).contains("ana@example.com").contains("Confirmá tu mail")
                .contains("http://localhost:5173/confirmar?t=ABC").contains("Gracias");
        assertThat(salida.getAll().lines().filter(l -> l.contains("Gracias")).count()).isEqualTo(1);
    }

    @Test
    void elEjecutorDeMailEsAcotado() {
        Executor ejecutor = new AsyncConfig().mailExecutor();

        assertThat(ejecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) ejecutor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(4);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("mail-");
    }

    @Test
    void conLaColaLlenaElEnvioSeDescartaYSeLogueaSinLanzar(CapturedOutput salida) throws Exception {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) new AsyncConfig().mailExecutor();
        pool.initialize();
        java.util.concurrent.CountDownLatch bloqueo = new java.util.concurrent.CountDownLatch(1);
        try {
            // 4 hilos ocupados + 100 en cola = 104; el 105 se rechaza.
            for (int i = 0; i < 104; i++) {
                pool.execute(() -> {
                    try {
                        bloqueo.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            assertThatCode(() -> pool.execute(() -> { })).doesNotThrowAnyException();
            assertThat(salida.getAll()).contains("cola de envío de mails está llena");
        } finally {
            bloqueo.countDown();
            pool.shutdown();
        }
    }

    @Test
    void unEnvioAsyncCorreEnElEjecutorDeMail() {
        new ApplicationContextRunner()
                .withUserConfiguration(AsyncConfig.class, ConfiguracionDePrueba.class)
                .run(ctx -> {
                    String hilo = ctx.getBean(EnviadorDePrueba.class).nombreDelHilo().get(5, TimeUnit.SECONDS);
                    assertThat(hilo).startsWith("mail-");
                });
    }

    @Configuration
    static class ConfiguracionDePrueba {
        @Bean
        EnviadorDePrueba enviadorDePrueba() {
            return new EnviadorDePrueba();
        }
    }

    static class EnviadorDePrueba {
        @Async("mailExecutor")
        public CompletableFuture<String> nombreDelHilo() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }
}
