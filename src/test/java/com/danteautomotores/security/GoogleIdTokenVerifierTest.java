package com.danteautomotores.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sin red ni cuenta de Google: los tokens se firman con un par RSA generado aquí y el verificador usa la clave
 * pública de ese par en lugar del JWKS de Google.
 */
class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "cliente-de-prueba.apps.googleusercontent.com";
    private static final String ISS = "https://accounts.google.com";

    private static KeyPair claveEsperada;
    private static KeyPair claveAjena;
    private static GoogleIdTokenVerifier verificador;

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        claveEsperada = generador.generateKeyPair();
        claveAjena = generador.generateKeyPair();
        verificador = verificadorPara(claveEsperada, CLIENT_ID);
    }

    private static GoogleIdTokenVerifier verificadorPara(KeyPair par, String clientId) {
        return new GoogleIdTokenVerifier(
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) par.getPublic()).build(), clientId);
    }

    private static JWTClaimsSet.Builder claimsValidos() {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISS)
                .audience(CLIENT_ID)
                .subject("sub-123")
                .issueTime(Date.from(ahora.minusSeconds(30)))
                .expirationTime(Date.from(ahora.plusSeconds(3600)))
                .claim("email", "Ana@Example.com")
                .claim("email_verified", true)
                .claim("given_name", "Ana")
                .claim("family_name", "Pérez")
                .claim("name", "Ana Pérez");
    }

    private static String firmar(JWTClaimsSet claims, KeyPair par) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) par.getPrivate()));
        return jwt.serialize();
    }

    private static String token(Consumer<JWTClaimsSet.Builder> ajuste) throws Exception {
        JWTClaimsSet.Builder claims = claimsValidos();
        ajuste.accept(claims);
        return firmar(claims.build(), claveEsperada);
    }

    private static void assertRechazado(String credential) {
        assertThatThrownBy(() -> verificador.verificar(credential))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unTokenValidoDevuelveLaIdentidad() throws Exception {
        IdentidadGoogle identidad = verificador.verificar(token(c -> { }));

        assertThat(identidad.sub()).isEqualTo("sub-123");
        assertThat(identidad.email()).isEqualTo("Ana@Example.com");
        assertThat(identidad.emailVerificado()).isTrue();
        assertThat(identidad.nombre()).isEqualTo("Ana");
        assertThat(identidad.apellido()).isEqualTo("Pérez");
        assertThat(identidad.nombreCompleto()).isEqualTo("Ana Pérez");
    }

    @Test
    void elEmisorSinEsquemaTambienSeAcepta() throws Exception {
        IdentidadGoogle identidad = verificador.verificar(token(c -> c.issuer("accounts.google.com")));

        assertThat(identidad.sub()).isEqualTo("sub-123");
    }

    @Test
    void unaAudienciaDeOtroClientIdSeRechaza() throws Exception {
        assertRechazado(token(c -> c.audience("otro-cliente.apps.googleusercontent.com")));
    }

    @Test
    void unTokenSinAudienciaSeRechaza() throws Exception {
        assertRechazado(token(c -> c.audience((String) null)));
    }

    @Test
    void unEmisorAjenoSeRechaza() throws Exception {
        assertRechazado(token(c -> c.issuer("https://accounts.google.com.evil.example")));
        assertRechazado(token(c -> c.issuer("https://login.evil.example")));
    }

    @Test
    void unTokenVencidoHaceMasDeSesentaSegundosSeRechaza() throws Exception {
        assertRechazado(token(c -> c.expirationTime(Date.from(Instant.now().minusSeconds(120)))));
    }

    @Test
    void unTokenVencidoHaceDiezSegundosSeTolera() throws Exception {
        IdentidadGoogle identidad = verificador.verificar(
                token(c -> c.expirationTime(Date.from(Instant.now().minusSeconds(10)))));

        assertThat(identidad.sub()).isEqualTo("sub-123");
    }

    @Test
    void unTokenMalformadoVacioONuloSeRechaza() {
        assertRechazado("esto-no-es-un-jwt");
        assertRechazado("a.b.c");
        assertRechazado("");
        assertRechazado("   ");
        assertRechazado(null);
    }

    @Test
    void unTokenFirmadoConOtraClaveRsaSeRechaza() throws Exception {
        assertRechazado(firmar(claimsValidos().build(), claveAjena));
    }

    @Test
    void unTokenSinSubSeRechaza() throws Exception {
        assertRechazado(token(c -> c.subject(null)));
    }

    @Test
    void conElClientIdEnBlancoNingunTokenPasa() throws Exception {
        GoogleIdTokenVerifier sinConfigurar = verificadorPara(claveEsperada, "");
        String credential = token(c -> { });

        assertThatThrownBy(() -> sinConfigurar.verificar(credential))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void emailVerifiedComoBooleanoOComoTextoTrueEsVerdadero() throws Exception {
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", true))).emailVerificado()).isTrue();
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", "true"))).emailVerificado()).isTrue();
    }

    @Test
    void emailVerifiedFalsoAusenteOCualquierOtroTextoEsFalso() throws Exception {
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", false))).emailVerificado()).isFalse();
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", null))).emailVerificado()).isFalse();
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", "false"))).emailVerificado()).isFalse();
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", "yes"))).emailVerificado()).isFalse();
        assertThat(verificador.verificar(token(c -> c.claim("email_verified", 1))).emailVerificado()).isFalse();
    }

    @Test
    void sinGivenNiFamilyNameElNombreSaleDeNameYElApellidoQuedaNulo() throws Exception {
        IdentidadGoogle identidad = verificador.verificar(token(c -> {
            c.claim("given_name", null);
            c.claim("family_name", null);
            c.claim("name", "Cher");
        }));

        assertThat(identidad.nombre()).isEqualTo("Cher");
        assertThat(identidad.apellido()).isNull();
    }

    @Test
    void sinNingunNombreLosCamposDeNombreQuedanNulos() throws Exception {
        IdentidadGoogle identidad = verificador.verificar(token(c -> {
            c.claim("given_name", null);
            c.claim("family_name", null);
            c.claim("name", null);
        }));

        assertThat(identidad.nombre()).isNull();
        assertThat(identidad.apellido()).isNull();
        assertThat(identidad.nombreCompleto()).isNull();
    }

    @Test
    void elMensajeDeLaExcepcionNoIncluyeElToken() throws Exception {
        String credential = firmar(claimsValidos().build(), claveAjena);

        assertThatThrownBy(() -> verificador.verificar(credential))
                .isInstanceOf(BadCredentialsException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain(credential);
                    assertThat(e.getMessage()).doesNotContain(credential.substring(0, 20));
                    assertThat(e.getCause()).isNull();
                });
    }
}
