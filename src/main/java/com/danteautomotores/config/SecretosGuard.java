package com.danteautomotores.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * Impide arrancar en producción con los valores por defecto del repo (públicos) para el secreto JWT
 * y la contraseña de la base, o con un secreto demasiado corto para HS256.
 * <p>
 * El modo permisivo (solo avisa en el log) es el que hay que pedir explícitamente: aplica cuando NO hay ningún perfil
 * activo (desarrollo local sin configuración: {@code mvn spring-boot:run} o el IDE) o cuando todos los perfiles
 * activos son de desarrollo ({@code dev}, {@code local}, {@code test}). Cualquier otro perfil ({@code prod},
 * {@code production}, {@code railway}, {@code staging}...) o una mezcla como {@code prod,dev} aborta el arranque.
 * Un deploy que no use el Dockerfile (que ya fija {@code SPRING_PROFILES_ACTIVE=prod}) debe definir un perfil.
 * Mismo criterio de fondo que {@link DataSeeder}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecretosGuard implements InitializingBean {

    static final String JWT_SECRET_POR_DEFECTO = "CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES";
    static final String DB_PASSWORD_POR_DEFECTO = "dante_dev_password";
    /** Lo mínimo que exige JJWT para firmar con HS256 (256 bits). */
    static final int JWT_SECRET_MIN_BYTES = 32;

    private static final Set<String> PERFILES_DE_DESARROLLO = Set.of("dev", "local", "test");

    private final Environment environment;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

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
    }

    private void fallarOAvisar(String mensaje) {
        if (!esEntornoDeDesarrollo()) {
            throw new IllegalStateException(mensaje);
        }
        log.warn("{} (fuera del modo desarrollo, es decir con cualquier perfil que no sea dev/local/test, el arranque se aborta)",
                mensaje);
    }

    // Sin perfiles activos cuenta como desarrollo (arranque local sin configuración); con perfiles, todos deben serlo.
    private boolean esEntornoDeDesarrollo() {
        return Arrays.stream(environment.getActiveProfiles()).allMatch(PERFILES_DE_DESARROLLO::contains);
    }
}
