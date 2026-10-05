package com.danteautomotores.security;

import com.danteautomotores.entity.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * UserDetails de una cuenta de la web. Suma el instante del último cambio de contraseña (claim "pca" del JWT)
 * para poder cerrar las sesiones anteriores a ese cambio.
 *
 * <p>Una cuenta que solo entra con Google no tiene contraseña: {@code User.builder().password(null)} lanza
 * {@code IllegalArgumentException}, y el filtro JWT lo captura y deja la request sin autenticar en silencio.
 * Por eso, sin contraseña se usa una constante que BCrypt nunca acepta (rechaza con false, sin lanzar).
 */
public class CuentaUserDetails extends User {

    /** Hash inválido: BCrypt devuelve false al compararlo, así que nadie entra por contraseña con él. */
    static final String HASH_INVALIDO = "!";

    private final long pcaSegundos;

    private CuentaUserDetails(String username, String password, String rol, long pcaSegundos) {
        super(username, password, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.pcaSegundos = pcaSegundos;
    }

    public static CuentaUserDetails de(Usuario usuario) {
        String hash = usuario.getPasswordHash() != null ? usuario.getPasswordHash() : HASH_INVALIDO;
        return new CuentaUserDetails(usuario.getEmail(), hash, usuario.getRol().name(),
                segundosDe(usuario.getPasswordCambiadaEn()));
    }

    /**
     * Segundos de época (UTC) del instante dado, 0 si es null. Se usa la misma función al emitir el token y al
     * validarlo, así la comparación es consistente sin importar la zona del servidor.
     */
    public static long segundosDe(LocalDateTime instante) {
        return instante == null ? 0L : instante.toEpochSecond(ZoneOffset.UTC);
    }

    public long getPcaSegundos() {
        return pcaSegundos;
    }
}
