package com.danteautomotores.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Ejecutor propio y acotado para los envíos de mail ({@code @Async("mailExecutor")}): la respuesta HTTP no espera a
 * Brevo y, como tarda lo mismo exista o no la cuenta, el tiempo de respuesta no delata si un mail está registrado.
 * Núcleo 2, máximo 4 y cola de 100: si se llena, el envío se rechaza antes que acumular hilos sin límite.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("mail-");
        // Con la cola llena el proxy de @Async lanzaría TaskRejectedException a quien llama (el registro o la
        // recuperación), y el método no puede atraparla: se descarta el envío y se loguea, sin datos personales.
        ejecutor.setRejectedExecutionHandler((tarea, pool) ->
                LOG.error("La cola de envío de mails está llena: se descarta un envío (el usuario puede reintentar)"));
        // Al apagar el back se deja terminar lo que ya está en cola (hasta 10 segundos).
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);
        return ejecutor;
    }
}
