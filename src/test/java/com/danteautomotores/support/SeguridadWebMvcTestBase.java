package com.danteautomotores.support;

import com.danteautomotores.config.SecurityConfig;
import com.danteautomotores.security.JwtAuthenticationFilter;
import com.danteautomotores.security.JwtService;
import com.danteautomotores.security.RestAccessDeniedHandler;
import com.danteautomotores.security.RestAuthenticationEntryPoint;
import com.danteautomotores.service.CatalogoService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.Mockito.when;

/**
 * Base para tests @WebMvcTest que ejercitan el SecurityConfig real y el filtro JWT real, sin base de datos.
 * Las subclases agregan su propio @WebMvcTest(controllers = ...) y los @MockBean de los services.
 */
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789",
        "app.jwt.expiration-ms=86400000",
        "app.cors.allowed-origins=http://localhost:5173"
})
public abstract class SeguridadWebMvcTestBase {

    protected static final String SECRETO = "0123456789012345678901234567890123456789";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JwtService jwtService;

    // CustomUserDetailsService no entra en el slice @WebMvcTest, así que se reemplaza por un mock.
    @MockBean
    protected UserDetailsService userDetailsService;

    // PublicacionController depende de CatalogoService; las clases de test que levantan ese controller lo necesitan.
    @MockBean
    protected CatalogoService catalogoService;

    /** Token válido para un usuario existente con el rol dado ("ADMIN" o "COMPRADOR"). */
    protected String bearerPara(String email, String rol) {
        UserDetails userDetails = User.withUsername(email).password("x").roles(rol).build();
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        return "Bearer " + jwtService.generateToken(userDetails);
    }

    /** Token bien firmado pero vencido hace una hora. */
    protected String bearerVencido(String email) {
        long ahora = System.currentTimeMillis();
        String token = Jwts.builder()
                .subject(email)
                .issuedAt(new Date(ahora - 2 * 60 * 60 * 1000L))
                .expiration(new Date(ahora - 60 * 60 * 1000L))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes()))
                .compact();
        return "Bearer " + token;
    }
}
