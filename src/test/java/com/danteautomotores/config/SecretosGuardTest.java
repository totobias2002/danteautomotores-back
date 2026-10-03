package com.danteautomotores.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class SecretosGuardTest {

    private static final String SECRETO_PROPIO = "un-secreto-propio-de-mas-de-treinta-y-dos-caracteres";
    private static final String DB_PROPIA = "clave-propia-de-la-base";

    private SecretosGuard guard(boolean prod, String jwtSecret, String dbPassword) {
        MockEnvironment environment = new MockEnvironment();
        if (prod) {
            environment.setActiveProfiles("prod");
        }
        SecretosGuard guard = new SecretosGuard(environment);
        ReflectionTestUtils.setField(guard, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(guard, "dbPassword", dbPassword);
        return guard;
    }

    @Test
    void prodConSecretosPropios_arranca() {
        assertThatCode(() -> guard(true, SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void prodConSecretoJwtDeEjemplo_noArranca() {
        assertThatThrownBy(() -> guard(true, SecretosGuard.JWT_SECRET_POR_DEFECTO, DB_PROPIA).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void prodSinSecretoJwt_noArranca() {
        assertThatThrownBy(() -> guard(true, "", DB_PROPIA).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void prodConPasswordDeBaseDeDesarrollo_noArranca() {
        assertThatThrownBy(() -> guard(true, SECRETO_PROPIO, SecretosGuard.DB_PASSWORD_POR_DEFECTO).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void sinProdConValoresPorDefecto_arrancaYSoloAvisa(CapturedOutput output) {
        assertThatCode(() -> guard(false, SecretosGuard.JWT_SECRET_POR_DEFECTO,
                SecretosGuard.DB_PASSWORD_POR_DEFECTO).afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("APP_JWT_SECRET").contains("SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void sinProdConSecretosPropios_noAvisa(CapturedOutput output) {
        guard(false, SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet();

        assertThat(output.getAll()).doesNotContain("APP_JWT_SECRET").doesNotContain("SPRING_DATASOURCE_PASSWORD");
    }
}
