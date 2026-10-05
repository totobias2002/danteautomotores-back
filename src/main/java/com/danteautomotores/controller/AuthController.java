package com.danteautomotores.controller;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.ConfirmarEmailRequest;
import com.danteautomotores.dto.auth.GoogleLoginRequest;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.OlvideContrasenaRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.dto.auth.RestablecerContrasenaRequest;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.security.ClienteIp;
import com.danteautomotores.service.AuthService;
import com.danteautomotores.service.GoogleAuthService;
import com.danteautomotores.service.LimitadorDeIntentos;
import com.danteautomotores.service.RecuperacionCuentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Endpoints públicos de la cuenta, con límites de intentos por mail y por IP (ajustables en las constantes).
 * Todos son POST: el consumo de un token con GET lo gastarían los escáneres de mail y el prefetch de links.
 * Las claves del limitador de login y de recuperación son distintas a propósito: un contador de login lleno no impide
 * recuperar la contraseña. La IP es de mejor esfuerzo (ver {@link ClienteIp}); el límite por mail es la defensa real.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String MENSAJE_DEMASIADOS_INTENTOS = "Hiciste demasiados intentos. Esperá unos minutos y volvé a probar.";
    static final String MENSAJE_MAIL_CONFIRMADO = "Tu mail quedó confirmado.";
    static final String MENSAJE_CONTRASENA_CAMBIADA = "Tu contraseña fue cambiada. Ya podés ingresar.";
    static final String MENSAJE_OLVIDE_CONTRASENA =
            "Si el mail tiene una cuenta, te mandamos un link para cambiar tu contraseña. Revisá también la carpeta de spam.";

    private static final Duration VENTANA_LOGIN = Duration.ofMinutes(15);
    private static final int MAX_LOGIN_FALLOS_POR_MAIL = 10;
    private static final int MAX_LOGIN_FALLOS_POR_IP = 30;

    private static final Duration VENTANA_REGISTRO = Duration.ofHours(1);
    private static final int MAX_REGISTROS_POR_IP = 10;

    private static final Duration VENTANA_GOOGLE = Duration.ofMinutes(15);
    private static final int MAX_GOOGLE_POR_IP = 30;

    private static final Duration VENTANA_TOKENS = Duration.ofMinutes(15);
    private static final int MAX_CONSUMO_DE_TOKENS_POR_IP = 30;

    private final AuthService authService;
    private final RecuperacionCuentaService recuperacionCuentaService;
    private final GoogleAuthService googleAuthService;
    private final LimitadorDeIntentos limitador;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request,
                                                 HttpServletRequest http) {
        // D-17: es la mitigación de enumeración del mensaje "Ya existe una cuenta con ese email".
        exigir(limitador.intentar("registro-ip:" + ClienteIp.de(http), MAX_REGISTROS_POR_IP, VENTANA_REGISTRO),
                VENTANA_REGISTRO);
        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String claveMail = "login:" + request.getEmail().trim().toLowerCase(Locale.ROOT);
        String claveIp = "login-ip:" + ClienteIp.de(http);

        // Solo cuentan los fallos de autenticación: una cuenta que escribe bien su contraseña nunca queda bloqueada.
        exigir(!limitador.bloqueado(claveMail, MAX_LOGIN_FALLOS_POR_MAIL, VENTANA_LOGIN)
                && !limitador.bloqueado(claveIp, MAX_LOGIN_FALLOS_POR_IP, VENTANA_LOGIN), VENTANA_LOGIN);

        AuthResponse sesion;
        try {
            sesion = authService.login(request);
        } catch (AuthenticationException e) {
            limitador.registrarFallo(claveMail, VENTANA_LOGIN);
            limitador.registrarFallo(claveIp, VENTANA_LOGIN);
            throw e;
        }
        limitador.olvidar(claveMail, VENTANA_LOGIN);
        return ResponseEntity.ok(sesion);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request,
                                               HttpServletRequest http) {
        exigir(limitador.intentar("google-ip:" + ClienteIp.de(http), MAX_GOOGLE_POR_IP, VENTANA_GOOGLE), VENTANA_GOOGLE);
        // Un token inválido sale como BadCredentialsException: el advice lo responde 401.
        return ResponseEntity.ok(googleAuthService.entrar(request.getCredential()));
    }

    @PostMapping("/confirmar-email")
    public ResponseEntity<Map<String, String>> confirmarEmail(@Valid @RequestBody ConfirmarEmailRequest request,
                                                              HttpServletRequest http) {
        exigirCupoDeTokens(http);
        recuperacionCuentaService.confirmarEmail(request.getToken());
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_MAIL_CONFIRMADO));
    }

    @PostMapping("/olvide-contrasena")
    public ResponseEntity<Map<String, String>> olvideContrasena(@Valid @RequestBody OlvideContrasenaRequest request,
                                                                HttpServletRequest http) {
        // D-14: siempre la misma respuesta, exista o no la cuenta y se haya excedido o no un límite.
        recuperacionCuentaService.solicitarRestablecimiento(request.getEmail(), ClienteIp.de(http));
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_OLVIDE_CONTRASENA));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Map<String, String>> restablecerContrasena(
            @Valid @RequestBody RestablecerContrasenaRequest request, HttpServletRequest http) {
        exigirCupoDeTokens(http);
        // No devuelve una sesión: el usuario va al login.
        recuperacionCuentaService.restablecerContrasena(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("mensaje", MENSAJE_CONTRASENA_CAMBIADA));
    }

    private void exigirCupoDeTokens(HttpServletRequest http) {
        exigir(limitador.intentar("token-ip:" + ClienteIp.de(http), MAX_CONSUMO_DE_TOKENS_POR_IP, VENTANA_TOKENS),
                VENTANA_TOKENS);
    }

    // Si no hay cupo se responde 429 con Retry-After igual a la ventana (cota superior de la espera real).
    private static void exigir(boolean hayCupo, Duration ventana) {
        if (!hayCupo) {
            throw new LimiteDeIntentosException(MENSAJE_DEMASIADOS_INTENTOS, ventana.toSeconds());
        }
    }
}
