package com.danteautomotores.controller;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.service.AuthService;
import com.danteautomotores.service.GoogleAuthService;
import com.danteautomotores.service.LimitadorDeIntentos;
import com.danteautomotores.service.RecuperacionCuentaService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los datos de prueba son ficticios. El limitador es el real y vive en el contexto compartido por todos los tests de la
 * clase: cada test usa su propia IP (X-Forwarded-For) y sus propios mails para no heredar contadores de otro.
 */
@WebMvcTest(AuthController.class)
@Import(LimitadorDeIntentos.class)
class AuthControllerTest extends SeguridadWebMvcTestBase {

    private static final String MENSAJE_OLVIDE =
            "Si el mail tiene una cuenta, te mandamos un link para cambiar tu contraseña. Revisá también la carpeta de spam.";
    private static final String TOKEN_VALIDO = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO1";
    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @MockBean
    private AuthService authService;
    @MockBean
    private RecuperacionCuentaService recuperacionCuentaService;
    @MockBean
    private GoogleAuthService googleAuthService;

    private String ip;
    private String mail;

    @BeforeEach
    void datosPropios() {
        int n = SECUENCIA.incrementAndGet();
        ip = "10.1." + (n / 250) + "." + (n % 250);
        mail = "persona" + n + "@x.com";
    }

    private AuthResponse sesion() {
        return AuthResponse.builder().token("jwt-ficticio").nombre("Ana").apellido("Pérez").email(mail)
                .rol("COMPRADOR").emailConfirmado(false).cuentaVerificada(false).faltantes(List.of()).build();
    }

    private static String json(String clave, String valor) {
        return "{\"" + clave + "\":\"" + valor + "\"}";
    }

    private ResultActions enviar(String ruta, String body) throws Exception {
        return enviar(ruta, body, ip);
    }

    private ResultActions enviar(String ruta, String body, String desdeIp) throws Exception {
        MockHttpServletRequestBuilder pedido = post(ruta).contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(pedido.header("X-Forwarded-For", desdeIp));
    }

    private ResultActions login(String paraMail, String password, String desdeIp) throws Exception {
        return enviar("/api/auth/login",
                "{\"email\":\"" + paraMail + "\",\"password\":\"" + password + "\"}", desdeIp);
    }

    private void loginFallido() {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("mal"));
    }

    private static String registroValido(String email) {
        return "{\"nombre\":\"Ana\",\"apellido\":\"Pérez\",\"email\":\"" + email + "\",\"password\":\"clave-segura-1\","
                + "\"telefono\":\"011 15 1234-5678\",\"dni\":\"30123456\"}";
    }

    // ---- públicos ----

    @Test
    void losEndpointsDeCuentaSonPublicosYSoloPost() throws Exception {
        // Sin token ninguno responde 401: llegan al controlador. Y un GET con token del link no existe (405).
        when(authService.login(any())).thenReturn(sesion());
        when(googleAuthService.entrar(anyString())).thenReturn(sesion());

        login(mail, "clave-segura-1", ip).andExpect(status().isOk());
        enviar("/api/auth/google", json("credential", "cred"), ip).andExpect(status().isOk());
        enviar("/api/auth/confirmar-email", json("token", TOKEN_VALIDO), ip).andExpect(status().isOk());
        enviar("/api/auth/olvide-contrasena", json("email", mail), ip).andExpect(status().isOk());
        enviar("/api/auth/restablecer-contrasena",
                "{\"token\":\"" + TOKEN_VALIDO + "\",\"password\":\"clave-segura-1\"}", ip).andExpect(status().isOk());

        mvc.perform(get("/api/auth/confirmar-email").param("token", TOKEN_VALIDO))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/auth/restablecer-contrasena").param("token", TOKEN_VALIDO))
                .andExpect(status().isMethodNotAllowed());
        verify(recuperacionCuentaService, times(1)).confirmarEmail(TOKEN_VALIDO);
    }

    // ---- login ----

    @Test
    void unLoginCorrectoDa200ConLaSesion() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sesion());

        login(mail, "clave-segura-1", ip)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-ficticio"));
    }

    @Test
    void elUndecimoLoginFallidoDelMismoMailDa429ConRetryAfter() throws Exception {
        loginFallido();

        for (int i = 0; i < 10; i++) {
            // IPs distintas: lo que se prueba acá es el límite por mail.
            login(mail, "mal", ip + "." + i).andExpect(status().isUnauthorized());
        }
        login(mail, "mal", ip + ".11")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.error").exists());

        // El bloqueo se decide antes de llamar al service: el undécimo intento ni siquiera lo toca.
        verify(authService, times(10)).login(any(LoginRequest.class));
    }

    @Test
    void elLimiteDeLoginNoDistingueMayusculasDelMail() throws Exception {
        loginFallido();
        for (int i = 0; i < 10; i++) {
            login(i % 2 == 0 ? mail.toUpperCase() : "  " + mail + " ", "mal", ip + "." + i)
                    .andExpect(status().isUnauthorized());
        }
        login(mail, "mal", ip + ".99").andExpect(status().isTooManyRequests());
    }

    @Test
    void unLoginCorrectoReiniciaElContadorYOtroMailNoEstaBloqueado() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenAnswer(inv -> {
            LoginRequest pedido = inv.getArgument(0);
            if ("bien".equals(pedido.getPassword())) {
                return sesion();
            }
            throw new BadCredentialsException("mal");
        });

        for (int i = 0; i < 9; i++) {
            login(mail, "mal", ip + "." + i).andExpect(status().isUnauthorized());
        }
        login(mail, "bien", ip + ".a").andExpect(status().isOk());
        for (int i = 0; i < 9; i++) {
            login(mail, "mal", ip + ".b" + i).andExpect(status().isUnauthorized());
        }
        // Sin el reinicio ya habría 18 fallos: acá todavía no está bloqueado.
        login(mail, "bien", ip + ".c").andExpect(status().isOk());

        // Y otra cuenta nunca estuvo bloqueada.
        for (int i = 0; i < 10; i++) {
            login(mail, "mal", ip + ".d" + i);
        }
        login(mail, "mal", ip + ".e").andExpect(status().isTooManyRequests());
        login("otra." + mail, "mal", ip + ".f").andExpect(status().isUnauthorized());
    }

    @Test
    void elTrigesimoPrimerFalloDesdeLaMismaIpDa429AunqueSeanMailsDistintos() throws Exception {
        loginFallido();

        for (int i = 0; i < 30; i++) {
            login("m" + i + "." + mail, "mal", ip).andExpect(status().isUnauthorized());
        }
        login("m99." + mail, "mal", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    // ---- registro ----

    @Test
    void elRegistroNumero11DesdeLaMismaIpDa429() throws Exception {
        when(authService.registrar(any())).thenReturn(sesion());

        for (int i = 0; i < 10; i++) {
            enviar("/api/auth/registro", registroValido("nuevo" + i + "." + mail)).andExpect(status().isOk());
        }
        enviar("/api/auth/registro", registroValido("nuevo99." + mail))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"));

        verify(authService, times(10)).registrar(any());
    }

    @Test
    void elRegistroDeOtraIpNoEstaBloqueado() throws Exception {
        when(authService.registrar(any())).thenReturn(sesion());
        for (int i = 0; i < 11; i++) {
            enviar("/api/auth/registro", registroValido("nuevo" + i + "." + mail));
        }

        enviar("/api/auth/registro", registroValido("otro." + mail), ip + ".otra").andExpect(status().isOk());
    }

    // ---- google ----

    @Test
    void googleConCredencialValidaDa200ConLaMismaSesion() throws Exception {
        when(googleAuthService.entrar("cred-ficticia")).thenReturn(sesion());

        enviar("/api/auth/google", json("credential", "cred-ficticia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-ficticio"))
                .andExpect(jsonPath("$.faltantes").isArray());
    }

    @Test
    void googleSinCredentialDa400ConCampos() throws Exception {
        enviar("/api/auth/google", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.credential").exists());

        verifyNoInteractions(googleAuthService);
    }

    @Test
    void googleConUnTokenInvalidoDa401ConError() throws Exception {
        when(googleAuthService.entrar(anyString()))
                .thenThrow(new BadCredentialsException("No pudimos verificar tu cuenta de Google"));

        enviar("/api/auth/google", json("credential", "cred-mala"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void googleUnaReglaDeLaPoliticaDa400ConSuMensaje() throws Exception {
        when(googleAuthService.entrar(anyString()))
                .thenThrow(new ReglaDeNegocioException("Google no confirmó tu mail: usá tu mail y contraseña."));

        enviar("/api/auth/google", json("credential", "cred"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Google no confirmó tu mail: usá tu mail y contraseña."));
    }

    @Test
    void googleElIntentoNumero31DesdeLaMismaIpDa429() throws Exception {
        when(googleAuthService.entrar(anyString())).thenReturn(sesion());

        for (int i = 0; i < 30; i++) {
            enviar("/api/auth/google", json("credential", "cred")).andExpect(status().isOk());
        }
        enviar("/api/auth/google", json("credential", "cred")).andExpect(status().isTooManyRequests());
    }

    // ---- confirmar mail y restablecer ----

    @Test
    void confirmarConTokenValidoDa200ConElMensaje() throws Exception {
        enviar("/api/auth/confirmar-email", json("token", TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu mail quedó confirmado."));

        verify(recuperacionCuentaService).confirmarEmail(TOKEN_VALIDO);
    }

    @Test
    void confirmarConTokenInvalidoDa400ConElMensajeDelService() throws Exception {
        doThrow(new ReglaDeNegocioException("El link no es válido o ya venció. Pedí uno nuevo."))
                .when(recuperacionCuentaService).confirmarEmail(anyString());

        enviar("/api/auth/confirmar-email", json("token", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El link no es válido o ya venció. Pedí uno nuevo."));
    }

    @Test
    void confirmarSinTokenDa400ConCampos() throws Exception {
        enviar("/api/auth/confirmar-email", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.token").exists());
    }

    @Test
    void elConsumoDeTokensTieneUnLimitePorIp() throws Exception {
        for (int i = 0; i < 30; i++) {
            enviar("/api/auth/confirmar-email", json("token", TOKEN_VALIDO)).andExpect(status().isOk());
        }
        // Confirmar y restablecer comparten el contador de consumo de tokens.
        enviar("/api/auth/restablecer-contrasena",
                "{\"token\":\"" + TOKEN_VALIDO + "\",\"password\":\"clave-segura-1\"}")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    @Test
    void restablecerConTokenValidoDa200SinSesion() throws Exception {
        enviar("/api/auth/restablecer-contrasena",
                "{\"token\":\"" + TOKEN_VALIDO + "\",\"password\":\"clave-segura-1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu contraseña fue cambiada. Ya podés ingresar."))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(recuperacionCuentaService).restablecerContrasena(TOKEN_VALIDO, "clave-segura-1");
        verifyNoInteractions(authService);
    }

    @Test
    void restablecerConContrasenaCortaDa400ConCamposYNoGastaElToken() throws Exception {
        enviar("/api/auth/restablecer-contrasena",
                "{\"token\":\"" + TOKEN_VALIDO + "\",\"password\":\"corta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").value("La contraseña debe tener entre 8 y 72 caracteres"));

        verify(recuperacionCuentaService, never()).restablecerContrasena(anyString(), anyString());
    }

    @Test
    void restablecerConTokenInvalidoDa400ConElMensajeGenerico() throws Exception {
        doThrow(new ReglaDeNegocioException("El link no es válido o ya venció. Pedí uno nuevo."))
                .when(recuperacionCuentaService).restablecerContrasena(anyString(), anyString());

        enviar("/api/auth/restablecer-contrasena",
                "{\"token\":\"" + TOKEN_VALIDO + "\",\"password\":\"clave-segura-1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El link no es válido o ya venció. Pedí uno nuevo."));
    }

    // ---- olvidé mi contraseña ----

    @Test
    void olvideContrasenaResponde200ConElMismoTextoParaDosMailsDistintos() throws Exception {
        enviar("/api/auth/olvide-contrasena", json("email", mail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDE));
        enviar("/api/auth/olvide-contrasena", json("email", "inexistente." + mail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDE));

        verify(recuperacionCuentaService).solicitarRestablecimiento(eq(mail), eq(ip));
        verify(recuperacionCuentaService).solicitarRestablecimiento(eq("inexistente." + mail), eq(ip));
    }

    @Test
    void olvideContrasenaNoSeBloqueaPorLosFallosDeLogin() throws Exception {
        loginFallido();
        for (int i = 0; i < 10; i++) {
            login(mail, "mal", ip + "." + i).andExpect(status().isUnauthorized());
        }
        // El login de ese mail ya está bloqueado...
        login(mail, "mal", ip + ".x").andExpect(status().isTooManyRequests());

        // ...pero recuperar la contraseña sigue respondiendo 200 y llamando al service (otras claves).
        enviar("/api/auth/olvide-contrasena", json("email", mail), ip + ".x")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDE));
        verify(recuperacionCuentaService).solicitarRestablecimiento(eq(mail), eq(ip + ".x"));
    }

    @Test
    void olvideContrasenaConMailMalFormadoDa400() throws Exception {
        enviar("/api/auth/olvide-contrasena", json("email", "no-es-un-mail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());

        verifyNoInteractions(recuperacionCuentaService);
    }

    @Test
    void sinHeaderDeProxyLaIpEsLaDireccionRemota() throws Exception {
        mvc.perform(post("/api/auth/olvide-contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", mail)))
                .andExpect(status().isOk());

        // MockMvc informa 127.0.0.1 como dirección remota.
        verify(recuperacionCuentaService).solicitarRestablecimiento(eq(mail), eq("127.0.0.1"));
    }

    @Test
    void conVariasIpsEnElHeaderSeUsaLaPrimera() throws Exception {
        enviar("/api/auth/olvide-contrasena", json("email", mail), "  203.0.113.7 , 10.0.0.1, 10.0.0.2");

        verify(recuperacionCuentaService).solicitarRestablecimiento(eq(mail), eq("203.0.113.7"));
    }
}
