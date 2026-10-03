package com.danteautomotores.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // Reloj inyectable: permite probar la regla de los 30 días de los vendidos (D-04) sin esperar. Misma zona que el
    // LocalDateTime.now() del @PrePersist de fechaPublicacion.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
