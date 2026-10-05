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

    private static final String CLOUDINARY_VALIDO = "credencial-cloudinary-propia";

    private static final String BREVO_VALIDO = "credencial-brevo-propia";
    private static final String REMITENTE_VALIDO = "dante@example.com";
    private static final String FRONT_VALIDO = "https://dante.example.com";
    private static final String GOOGLE_VALIDO = "client-id-de-google-propio";

    private SecretosGuard guard(String jwtSecret, String dbPassword, String... perfiles) {
        return guardConCloudinary(CLOUDINARY_VALIDO, CLOUDINARY_VALIDO, CLOUDINARY_VALIDO, jwtSecret, dbPassword, perfiles);
    }

    private SecretosGuard guardConCloudinary(String cloudName, String apiKey, String apiSecret,
                                             String jwtSecret, String dbPassword, String... perfiles) {
        MockEnvironment environment = new MockEnvironment();
        if (perfiles.length > 0) {
            environment.setActiveProfiles(perfiles);
        }
        SecretosGuard guard = new SecretosGuard(environment);
        ReflectionTestUtils.setField(guard, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(guard, "dbPassword", dbPassword);
        ReflectionTestUtils.setField(guard, "cloudinaryCloudName", cloudName);
        ReflectionTestUtils.setField(guard, "cloudinaryApiKey", apiKey);
        ReflectionTestUtils.setField(guard, "cloudinaryApiSecret", apiSecret);
        ReflectionTestUtils.setField(guard, "brevoApiKey", BREVO_VALIDO);
        ReflectionTestUtils.setField(guard, "mailRemitenteEmail", REMITENTE_VALIDO);
        ReflectionTestUtils.setField(guard, "frontendUrl", FRONT_VALIDO);
        ReflectionTestUtils.setField(guard, "googleClientId", GOOGLE_VALIDO);
        return guard;
    }

    /** Guard con todo en orden salvo los cuatro campos de mail e identidad, que se pisan. */
    private SecretosGuard guardConMail(String brevo, String remitente, String frontUrl, String googleClientId,
                                       String... perfiles) {
        SecretosGuard guard = guard(SECRETO_PROPIO, DB_PROPIA, perfiles);
        ReflectionTestUtils.setField(guard, "brevoApiKey", brevo);
        ReflectionTestUtils.setField(guard, "mailRemitenteEmail", remitente);
        ReflectionTestUtils.setField(guard, "frontendUrl", frontUrl);
        ReflectionTestUtils.setField(guard, "googleClientId", googleClientId);
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

    @Test
    void prodConCloudinaryCompleto_arranca() {
        assertThatCode(() -> guardConCloudinary("mi-nube", "123456", "secreto-cloud", SECRETO_PROPIO, DB_PROPIA, "prod")
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cloudName", "apiKey", "apiSecret"})
    void prodConUnaCredencialDeCloudinaryVacia_noArranca(String faltante) {
        assertThatThrownBy(() -> guardConCloudinary(
                "cloudName".equals(faltante) ? "" : CLOUDINARY_VALIDO,
                "apiKey".equals(faltante) ? "" : CLOUDINARY_VALIDO,
                "apiSecret".equals(faltante) ? "" : CLOUDINARY_VALIDO,
                SECRETO_PROPIO, DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDINARY")
                .hasMessageNotContaining(CLOUDINARY_VALIDO);
    }

    @Test
    void prodConCredencialesDeCloudinaryEnBlancoONulas_noArranca() {
        assertThatThrownBy(() -> guardConCloudinary("   ", CLOUDINARY_VALIDO, CLOUDINARY_VALIDO,
                SECRETO_PROPIO, DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDINARY_CLOUD_NAME");
        assertThatThrownBy(() -> guardConCloudinary(CLOUDINARY_VALIDO, null, CLOUDINARY_VALIDO,
                SECRETO_PROPIO, DB_PROPIA, "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDINARY_API_KEY");
    }

    @Test
    void prodYDevMezcladosSinCloudinary_noArranca() {
        assertThatThrownBy(() -> guardConCloudinary("", "", "", SECRETO_PROPIO, DB_PROPIA, "prod", "dev")
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDINARY");
    }

    @Test
    void sinPerfilSinCloudinary_arrancaYAvisa(CapturedOutput output) {
        assertThatCode(() -> guardConCloudinary("", "", "", SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("CLOUDINARY");
    }

    @Test
    void perfilDevSinCloudinary_arrancaYAvisa(CapturedOutput output) {
        assertThatCode(() -> guardConCloudinary("", "", "", SECRETO_PROPIO, DB_PROPIA, "dev").afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("CLOUDINARY");
    }

    @Test
    void sinPerfilConCloudinaryCompleto_noAvisaDeCloudinary(CapturedOutput output) {
        guard(SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet();

        assertThat(output.getAll()).doesNotContain("CLOUDINARY");
    }

    @Test
    void prodConMailEIdentidadCompletos_arranca() {
        assertThatCode(() -> guardConMail(BREVO_VALIDO, REMITENTE_VALIDO, FRONT_VALIDO, GOOGLE_VALIDO, "prod")
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"BREVO_API_KEY", "MAIL_REMITENTE_EMAIL", "APP_FRONTEND_URL", "GOOGLE_CLIENT_ID"})
    void prodSinUnaDeLasCuatroVariablesNuevas_noArrancaYElMensajeLaNombra(String variable) {
        assertThatThrownBy(() -> guardConMail(
                "BREVO_API_KEY".equals(variable) ? "" : BREVO_VALIDO,
                "MAIL_REMITENTE_EMAIL".equals(variable) ? "  " : REMITENTE_VALIDO,
                "APP_FRONTEND_URL".equals(variable) ? "" : FRONT_VALIDO,
                "GOOGLE_CLIENT_ID".equals(variable) ? null : GOOGLE_VALIDO,
                "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(variable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "https://localhost", "https://127.0.0.1:5173",
            "http://dante.example.com", "dante.example.com", "HTTP://dante.example.com"})
    void prodConFrontNoHttpsOLocalhost_noArranca(String url) {
        assertThatThrownBy(() -> guardConMail(BREVO_VALIDO, REMITENTE_VALIDO, url, GOOGLE_VALIDO, "prod")
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_FRONTEND_URL");
    }

    @Test
    void prodYDevMezcladosSinBrevo_noArranca() {
        assertThatThrownBy(() -> guardConMail("", REMITENTE_VALIDO, FRONT_VALIDO, GOOGLE_VALIDO, "prod", "dev")
                .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }

    @Test
    void sinPerfilSinMailNiIdentidad_arrancaYAvisaDeCadaVariable(CapturedOutput output) {
        assertThatCode(() -> guardConMail("", "", "http://localhost:5173", "").afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("BREVO_API_KEY").contains("MAIL_REMITENTE_EMAIL")
                .contains("APP_FRONTEND_URL").contains("GOOGLE_CLIENT_ID");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "local", "test"})
    void perfilDeDesarrolloSinBrevo_arrancaYAvisa(String perfil, CapturedOutput output) {
        assertThatCode(() -> guardConMail("", REMITENTE_VALIDO, FRONT_VALIDO, GOOGLE_VALIDO, perfil)
                .afterPropertiesSet())
                .doesNotThrowAnyException();

        assertThat(output.getAll()).contains("BREVO_API_KEY");
    }

    @Test
    void sinPerfilConMailEIdentidadCompletos_noAvisaDeEllos(CapturedOutput output) {
        guard(SECRETO_PROPIO, DB_PROPIA).afterPropertiesSet();

        assertThat(output.getAll()).doesNotContain("BREVO_API_KEY").doesNotContain("MAIL_REMITENTE_EMAIL")
                .doesNotContain("APP_FRONTEND_URL").doesNotContain("GOOGLE_CLIENT_ID");
    }

    @Test
    void elMensajeDeFallaNoFiltraElValorDeLaVariable() {
        assertThatThrownBy(() -> guardConMail(BREVO_VALIDO, REMITENTE_VALIDO, "http://localhost:5173", GOOGLE_VALIDO,
                "prod").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(BREVO_VALIDO);
    }
}
