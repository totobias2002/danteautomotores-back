package com.danteautomotores.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Verifica el ID token que el botón oficial de Google entrega al navegador: firma contra el JWKS de Google,
 * emisor, audiencia (el Client ID configurado) y vencimiento con 60 segundos de tolerancia.
 *
 * <p>Nunca se loguea el token ni los datos de la persona, y la excepción lleva un mensaje fijo.
 */
@Component
public class GoogleIdTokenVerifier {

    static final String JWKS_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String MENSAJE = "No pudimos verificar tu cuenta de Google";
    private static final Duration TOLERANCIA = Duration.ofSeconds(60);

    private final NimbusJwtDecoder decoder;

    /**
     * Para producción: RS256 por defecto, caché de 5 minutos del JWKS y rotación de claves automática. No baja
     * nada hasta el primer uso. Con el Client ID en blanco (desarrollo sin configurar) ningún token pasa.
     */
    @Autowired
    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this(NimbusJwtDecoder.withJwkSetUri(JWKS_GOOGLE).build(), clientId);
    }

    /** Para tests: recibe un decoder con la clave pública de un par RSA generado, sin red. */
    GoogleIdTokenVerifier(NimbusJwtDecoder decoder, String clientId) {
        OAuth2TokenValidator<Jwt> emisor = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return ("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "emisor", null));
        };
        OAuth2TokenValidator<Jwt> audiencia = jwt ->
                (clientId != null && !clientId.isBlank() && jwt.getAudience() != null
                        && jwt.getAudience().contains(clientId))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "audiencia", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(TOLERANCIA), emisor, audiencia));
        this.decoder = decoder;
    }

    /** Devuelve la identidad del token o lanza BadCredentialsException con un mensaje fijo (sin el token). */
    public IdentidadGoogle verificar(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new BadCredentialsException(MENSAJE);
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(credential);
        } catch (RuntimeException e) {
            // JwtException (firma, claims, formato) o un fallo de red al bajar el JWKS: todo es un rechazo.
            throw new BadCredentialsException(MENSAJE);
        }
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new BadCredentialsException(MENSAJE);
        }
        String nombreCompleto = textoONulo(jwt.getClaim("name"));
        String nombre = textoONulo(jwt.getClaim("given_name"));
        return new IdentidadGoogle(
                sub,
                textoONulo(jwt.getClaim("email")),
                emailVerificado(jwt.getClaim("email_verified")),
                nombre != null ? nombre : nombreCompleto,
                textoONulo(jwt.getClaim("family_name")),
                nombreCompleto);
    }

    // Google manda un booleano, pero algunos emisores lo mandan como texto: se acepta solo true o "true".
    private static boolean emailVerificado(Object valor) {
        return Boolean.TRUE.equals(valor) || (valor instanceof String s && "true".equalsIgnoreCase(s.trim()));
    }

    private static String textoONulo(Object valor) {
        if (valor instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return null;
    }
}
