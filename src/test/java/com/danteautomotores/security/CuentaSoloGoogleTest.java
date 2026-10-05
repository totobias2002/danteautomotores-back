package com.danteautomotores.security;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Una cuenta sin contraseña (solo Google) se autentica con su JWT. Antes, un password nulo hacía lanzar
 * IllegalArgumentException al armar el UserDetails y el filtro dejaba la request sin autenticar en silencio.
 */
class CuentaSoloGoogleTest {

    private UsuarioRepository usuarioRepository;
    private CustomUserDetailsService userDetailsService;
    private JwtService jwtService;
    private Usuario cuentaGoogle;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        userDetailsService = new CustomUserDetailsService(usuarioRepository);
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "0123456789012345678901234567890123456789");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);

        cuentaGoogle = Usuario.builder()
                .id(7L)
                .nombre("Ana")
                .email("ana@x.com")
                .passwordHash(null)
                .rol(Rol.COMPRADOR)
                .googleSub("sub-123")
                .build();
        when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(cuentaGoogle));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cargarUnaCuentaSinContrasenaNoLanzaYDevuelveLaContrasenaInvalida() {
        UserDetails cuenta = userDetailsService.loadUserByUsername("ana@x.com");

        assertThat(cuenta).isInstanceOf(CuentaUserDetails.class);
        assertThat(cuenta.getUsername()).isEqualTo("ana@x.com");
        assertThat(cuenta.getPassword()).isEqualTo(CuentaUserDetails.HASH_INVALIDO);
        assertThat(cuenta.getAuthorities()).extracting("authority").containsExactly("ROLE_COMPRADOR");
    }

    @Test
    void laContrasenaInvalidaNuncaCoincideConNingunaContrasena() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        assertThat(bcrypt.matches("!", CuentaUserDetails.HASH_INVALIDO)).isFalse();
        assertThat(bcrypt.matches("", CuentaUserDetails.HASH_INVALIDO)).isFalse();
        assertThat(bcrypt.matches("cualquiera", CuentaUserDetails.HASH_INVALIDO)).isFalse();
    }

    @Test
    void elFiltroRealAutenticaALaCuentaSinContrasenaConSuRol() throws Exception {
        String token = jwtService.generateToken(userDetailsService.loadUserByUsername("ana@x.com"));
        JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filtro.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getName()).isEqualTo("ana@x.com");
        assertThat(autenticacion.getAuthorities()).extracting("authority").containsExactly("ROLE_COMPRADOR");
    }

    @Test
    void elFiltroNoAutenticaUnTokenAnteriorAlCambioDeContrasena() throws Exception {
        String tokenViejo = jwtService.generateToken(userDetailsService.loadUserByUsername("ana@x.com"));
        cuentaGoogle.setPasswordCambiadaEn(java.time.LocalDateTime.now().plusMinutes(1));
        JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenViejo);

        filtro.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
