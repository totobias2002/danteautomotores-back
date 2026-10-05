package com.danteautomotores.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Instante (segundos de época) del último cambio de contraseña de la cuenta al emitir el token.
    static final String CLAIM_PCA = "pca";

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Todo token lleva pca, así ningún emisor puede saltearse el cierre de sesiones por cambio de contraseña.
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(CLAIM_PCA, pcaDe(userDetails));
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // D-19: un token emitido antes del último cambio de contraseña deja de valer. Sin claim (tokens
        // anteriores a esta fase) equivale a 0, y vale mientras la cuenta nunca haya cambiado la contraseña.
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && pcaDelToken(token) >= pcaDe(userDetails);
    }

    private static long pcaDe(UserDetails userDetails) {
        return userDetails instanceof CuentaUserDetails cuenta ? cuenta.getPcaSegundos() : 0L;
    }

    private long pcaDelToken(String token) {
        Number pca = extractClaim(token, claims -> claims.get(CLAIM_PCA, Number.class));
        return pca == null ? 0L : pca.longValue();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
