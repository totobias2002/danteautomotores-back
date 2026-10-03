package com.danteautomotores.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Impide arrancar en producción con los valores por defecto del repo (públicos) para el secreto JWT
 * y la contraseña de la base. En desarrollo la app sigue arrancando sin configuración extra y solo avisa.
 * Mismo criterio que {@link DataSeeder}: con el perfil {@code prod} aborta el arranque, sin él avisa en el log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecretosGuard implements InitializingBean {

    static final String JWT_SECRET_POR_DEFECTO = "CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES";
    static final String DB_PASSWORD_POR_DEFECTO = "dante_dev_password";

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank() || JWT_SECRET_POR_DEFECTO.equals(jwtSecret)) {
            fallarOAvisar("APP_JWT_SECRET falta o es el valor de ejemplo público: definí un secreto propio de al menos 32 caracteres");
        }
        if (DB_PASSWORD_POR_DEFECTO.equals(dbPassword)) {
            fallarOAvisar("SPRING_DATASOURCE_PASSWORD es la contraseña de desarrollo del repo: definí una contraseña propia");
        }
    }

    private void fallarOAvisar(String mensaje) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(mensaje);
        }
        log.warn("{} (en producción, con el perfil prod, el arranque se aborta)", mensaje);
    }
}
