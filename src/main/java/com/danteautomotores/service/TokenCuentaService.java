package com.danteautomotores.service;

import com.danteautomotores.entity.TokenCuenta;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.repository.TokenCuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Tokens opacos de un solo uso para confirmar el mail (24 h) y restablecer la contraseña (1 h). El token que viaja en
 * el link son 32 bytes de SecureRandom (256 bits, 43 caracteres en Base64 URL); en la base solo se guarda su SHA-256,
 * así que un volcado de la tabla no sirve para armar un link. Con esa entropía no hace falta sal.
 */
@Service
@RequiredArgsConstructor
public class TokenCuentaService {

    static final int BYTES_DEL_TOKEN = 32;
    static final int LARGO_DEL_TOKEN = 43;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TokenCuentaRepository tokenCuentaRepository;
    private final Clock clock;

    @Value("${app.seguridad.confirmacion-email-horas:24}")
    private long confirmacionEmailHoras = 24;

    @Value("${app.seguridad.restablecer-contrasena-minutos:60}")
    private long restablecerContrasenaMinutos = 60;

    /**
     * Emite un token nuevo para la cuenta y el tipo y devuelve el valor que va en la URL. Borra antes los anteriores
     * del mismo tipo: hay un solo link vivo por vez.
     */
    @Transactional
    public String emitir(Long usuarioId, TipoTokenCuenta tipo) {
        tokenCuentaRepository.deleteByUsuarioIdAndTipo(usuarioId, tipo);

        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime ahora = LocalDateTime.now(clock);
        tokenCuentaRepository.save(TokenCuenta.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .tokenHash(hashear(token))
                .creadoEn(ahora)
                .expiraEn(ahora.plus(vigencia(tipo)))
                .build());
        return token;
    }

    /**
     * Devuelve el id de la cuenta solo si el token era válido para ese tipo y este pedido lo ganó. Vencido, usado,
     * de otro tipo o inexistente dan el mismo resultado vacío.
     */
    @Transactional
    public Optional<Long> consumir(String token, TipoTokenCuenta tipo) {
        if (!tieneFormatoValido(token)) {
            return Optional.empty();
        }
        String hash = hashear(token);
        int filas = tokenCuentaRepository.consumir(hash, tipo, LocalDateTime.now(clock));
        if (filas != 1) {
            return Optional.empty();
        }
        return tokenCuentaRepository.findByTokenHash(hash).map(TokenCuenta::getUsuarioId);
    }

    /** Borra los tokens pendientes de la cuenta, de los dos tipos. */
    @Transactional
    public void descartarPendientes(Long usuarioId) {
        tokenCuentaRepository.deleteByUsuarioId(usuarioId);
    }

    private Duration vigencia(TipoTokenCuenta tipo) {
        return switch (tipo) {
            case CONFIRMAR_EMAIL -> Duration.ofHours(confirmacionEmailHoras);
            case RESTABLECER_CONTRASENA -> Duration.ofMinutes(restablecerContrasenaMinutos);
        };
    }

    // Un token que no tiene la forma de los que emitimos ni se busca en la base.
    private static boolean tieneFormatoValido(String token) {
        if (token == null || token.length() != LARGO_DEL_TOKEN) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean valido = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            if (!valido) {
                return false;
            }
        }
        return true;
    }

    private static String hashear(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está garantizado en todo JDK.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
