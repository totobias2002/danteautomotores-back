package com.danteautomotores.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    private SecretosGuard guard(String jwtSecret, String dbPassword, String... perfiles) {
        MockEnvironment environment = new MockEnvironment();
        if (perfiles.length > 0) {
            environment.setActiveProfiles(perfiles);
        }
        SecretosGuard guard = new SecretosGuard(environment);
        ReflectionTestUtils.setField(guard, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(guard, "dbPassword", dbPassword);
        return guard;
    }

    @Test
    void prodConSecretosPropios_arranca() {
        assertThatCode(() -> guard(SECRETO_PROPIO, DB_PROPIA, "prod").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void prodConSecretoJwtDeEjemplo_noArranca() {
        assertThatThrownBy(() -> guard(SecretosGuard.JWT_SECRET_POR_DEFECTO, DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void prodSinSecretoJwt_noArranca() {
        assertThatThrownBy(() -> guard("", DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void prodConPasswordDeBaseDeDesarrollo_noArranca() {
        assertThatThrownBy(() -> guard(SECRETO_PROPIO, SecretosGuard.DB_PASSWORD_POR_DEFECTO, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void prodConSecretoJwtCorto_noArranca() {
        assertThatThrownBy(() -> guard("abc", DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET")
                .hasMessageContaining("32");
    }

    @Test
    void secretoDeExactamente32Bytes_esValido() {
        assertThatCode(() -> guard("x".repeat(32), DB_PROPIA, "prod").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void elLargoSeMideEnBytesNoEnCaracteres() {
        // 16 caracteres de 2 bytes cada uno en UTF-8 = 32 bytes: alcanza. 15 de ellos = 30 bytes: no.
        assertThatCode(() -> guard("ñ".repeat(16), DB_PROPIA, "prod").afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> guard("ñ".repeat(15), DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"production", "railway", "staging", "qa"})
    void cualquierPerfilQueNoSeaDeDesarrolloEsEstricto(String perfil) {
        assertThatThrownBy(() -> guard(SecretosGuard.JWT_SECRET_POR_DEFECTO,
                SecretosGuard.DB_PASSWORD_POR_DEFECTO, perfil).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mezclarProdConUnPerfilDeDesarrolloSigueSiendoEstricto() {
        assertThatThrownBy(() -> guard(SecretosGuard.JWT_SECRET_POR_DEFECTO,
                SecretosGuard.DB_PASSWORD_POR_DEFECTO, "prod", "dev").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinPerfilConValoresPorDefecto_arrancaYSoloAvisa(CapturedOutput output) {
        assertThatCode(() -> guard(SecretosGuard.JWT_SECRET_POR_DEFECTO,
                SecretosGuard.DB_PASSWORD_POR_DEFECTO).afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("APP_JWT_SECRET").contains("SPRING_DATASOURCE_PASSWORD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "local", "test"})
    void perfilDeDesarrolloConValoresPorDefecto_arrancaYSoloAvisa(String perfil, CapturedOutput output) {
        assertThatCode(() -> guard(SecretosGuard.JWT_SECRET_POR_DEFECTO,
                SecretosGuard.DB_PASSWORD_POR_DEFECTO, perfil).afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("APP_JWT_SECRET");
    }

    @Test
    void sinPerfilConSecretoCorto_arrancaYAvisa(CapturedOutput output) {
        assertThatCode(() -> guard("abc", DB_PROPIA).afterPropertiesSet()).doesNotThrowAnyException();

        assertThat(output.getAll()).contains("menos de 32 bytes");
    }

    @Test
    void sinPerfilConSecretosPropios_noAvisa(CapturedOutput output) {
        guard(SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet();

        assertThat(output.getAll()).doesNotContain("APP_JWT_SECRET").doesNotContain("SPRING_DATASOURCE_PASSWORD");
    }
}
