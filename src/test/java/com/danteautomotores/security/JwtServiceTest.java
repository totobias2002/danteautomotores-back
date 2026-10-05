package com.danteautomotores.security;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-19: cambiar la contraseña cierra las demás sesiones (claim pca) sin romper los tokens ya emitidos.
 */
class JwtServiceTest {

    private static final String SECRETO = "0123456789012345678901234567890123456789";

    private JwtService jwtService;

    @BeforeEach
    void preparar() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRETO);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);
    }

    private static CuentaUserDetails cuentaConPca(long segundos) {
        Usuario usuario = Usuario.builder()
                .email("ana@x.com")
                .passwordHash("hash")
                .rol(Rol.COMPRADOR)
                .passwordCambiadaEn(segundos == 0 ? null : LocalDateTime.ofEpochSecond(segundos, 0, ZoneOffset.UTC))
                .build();
        return CuentaUserDetails.de(usuario);
    }

    @Test
    void tokenDeCuentaSinCambioDeContrasenaEsValidoParaEsaCuenta() {
        CuentaUserDetails cuenta = cuentaConPca(0);

        String token = jwtService.generateToken(cuenta);

        assertThat(jwtService.isTokenValid(token, cuenta)).isTrue();
    }

    @Test
    void tokenConPca100ValeParaCuentasConPca100YConPca50() {
        String token = jwtService.generateToken(cuentaConPca(100));

        assertThat(jwtService.isTokenValid(token, cuentaConPca(100))).isTrue();
        assertThat(jwtService.isTokenValid(token, cuentaConPca(50))).isTrue();
    }

    @Test
    void tokenConPca100NoValeParaUnaCuentaQueCambioLaContrasenaDespues() {
        String token = jwtService.generateToken(cuentaConPca(100));

        assertThat(jwtService.isTokenValid(token, cuentaConPca(200))).isFalse();
    }

    @Test
    void tokenEmitidoDespuesDelCambioValeYElAnteriorNo() {
        String viejo = jwtService.generateToken(cuentaConPca(0));
        CuentaUserDetails despuesDelCambio = cuentaConPca(1_700_000_000L);

        String nuevo = jwtService.generateToken(despuesDelCambio);

        assertThat(jwtService.isTokenValid(viejo, despuesDelCambio)).isFalse();
        assertThat(jwtService.isTokenValid(nuevo, despuesDelCambio)).isTrue();
    }

    @Test
    void tokenSinClaimPcaValeSoloParaCuentaQueNuncaCambioLaContrasena() {
        String sinClaim = Jwts.builder()
                .subject("ana@x.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(sinClaim, cuentaConPca(0))).isTrue();
        assertThat(jwtService.isTokenValid(sinClaim, cuentaConPca(1))).isFalse();
    }

    @Test
    void tokenParaUnUserPlanoSigueSiendoValido() {
        UserDetails plano = User.withUsername("ana@x.com").password("x").roles("COMPRADOR").build();

        String token = jwtService.generateToken(plano);

        assertThat(jwtService.isTokenValid(token, plano)).isTrue();
    }

    @Test
    void tokenEmitidoParaUnUserPlanoNoValeSiLaCuentaYaCambioLaContrasena() {
        UserDetails plano = User.withUsername("ana@x.com").password("x").roles("COMPRADOR").build();

        String token = jwtService.generateToken(plano);

        assertThat(jwtService.isTokenValid(token, cuentaConPca(10))).isFalse();
    }

    @Test
    void tokenDeOtroUsuarioNoVale() {
        String token = jwtService.generateToken(cuentaConPca(0));
        UserDetails otro = User.withUsername("otra@x.com").password("x").roles("COMPRADOR").build();

        assertThat(jwtService.isTokenValid(token, otro)).isFalse();
    }
}
