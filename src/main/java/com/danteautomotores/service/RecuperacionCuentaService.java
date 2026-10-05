package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Confirmación de mail y recuperación de contraseña (AUTH-04). Nunca loguea el token, el mail ni la IP.
 *
 * <ul>
 *   <li>El mensaje de un link inválido es el mismo para vencido, usado e inexistente.</li>
 *   <li>Pedir el cambio de contraseña (D-14) no distingue si la cuenta existe ni si se pasó un límite: el método
 *       vuelve igual en todos los casos y el mail sale por el ejecutor asíncrono.</li>
 *   <li>Restablecer no devuelve una sesión (OWASP: no autenticar automáticamente tras crear la contraseña): el usuario
 *       va al login. Sí cierra las demás sesiones (D-19) fijando {@code passwordCambiadaEn}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RecuperacionCuentaService {

    public static final String MENSAJE_LINK_INVALIDO = "El link no es válido o ya venció. Pedí uno nuevo.";

    static final int MAX_SOLICITUDES_POR_MAIL = 3;
    static final int MAX_SOLICITUDES_POR_IP = 10;
    static final Duration VENTANA_SOLICITUDES = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final TokenCuentaService tokenCuentaService;
    private final NotificacionesService notificacionesService;
    private final LimitadorDeIntentos limitador;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /** Confirma el mail con el token del link (24 h, un solo uso). Idempotente sobre la cuenta. */
    @Transactional
    public void confirmarEmail(String token) {
        Long usuarioId = tokenCuentaService.consumir(token, TipoTokenCuenta.CONFIRMAR_EMAIL)
                .orElseThrow(() -> new ReglaDeNegocioException(MENSAJE_LINK_INVALIDO));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaDeNegocioException(MENSAJE_LINK_INVALIDO));
        if (!usuario.isEmailConfirmado()) {
            usuario.setEmailConfirmado(true);
            usuarioRepository.save(usuario);
        }
    }

    /**
     * D-14: nunca lanza por una cuenta inexistente ni por un límite excedido. Deliberadamente sin {@code @Transactional}:
     * el token queda confirmado en la base antes de que salga el mail con el link.
     */
    public void solicitarRestablecimiento(String email, String ip) {
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        // Se consultan los dos contadores siempre (sin cortocircuito) para que ambos cuenten cada pedido.
        boolean dentroDelLimiteDelMail = limitador.intentar("reset:" + mail, MAX_SOLICITUDES_POR_MAIL, VENTANA_SOLICITUDES);
        boolean dentroDelLimiteDeLaIp = limitador.intentar("reset-ip:" + ip, MAX_SOLICITUDES_POR_IP, VENTANA_SOLICITUDES);
        if (!dentroDelLimiteDelMail || !dentroDelLimiteDeLaIp) {
            return;
        }

        Optional<Usuario> cuenta = usuarioRepository.findByEmailIgnoreCase(mail);
        if (cuenta.isEmpty()) {
            return;
        }
        Usuario usuario = cuenta.get();
        String token = tokenCuentaService.emitir(usuario.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);
        notificacionesService.enviarRestablecerContrasena(usuario, token);
    }

    /**
     * Cambia la contraseña con un token de restablecimiento. Una cuenta solo-Google puede definir una por esta vía y
     * queda con los dos métodos. Llegar al link prueba que controla la casilla: el mail queda confirmado.
     */
    @Transactional
    public void restablecerContrasena(String token, String nueva) {
        // Primero el largo: una contraseña inválida no gasta el token.
        ContrasenaCuenta.verificarLargo(nueva);

        Long usuarioId = tokenCuentaService.consumir(token, TipoTokenCuenta.RESTABLECER_CONTRASENA)
                .orElseThrow(() -> new ReglaDeNegocioException(MENSAJE_LINK_INVALIDO));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaDeNegocioException(MENSAJE_LINK_INVALIDO));

        usuario.setPasswordHash(passwordEncoder.encode(nueva));
        ContrasenaCuenta.marcarCambio(usuario, clock);
        usuario.setEmailConfirmado(true);
        usuarioRepository.save(usuario);
        tokenCuentaService.descartarPendientes(usuario.getId());

        // El aviso sale cuando el cambio ya está confirmado en la base.
        ContrasenaCuenta.despuesDelCommit(() -> notificacionesService.enviarContrasenaCambiada(usuario));
    }
}
