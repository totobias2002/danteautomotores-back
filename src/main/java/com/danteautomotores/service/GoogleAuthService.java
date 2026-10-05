package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.GoogleIdTokenVerifier;
import com.danteautomotores.security.IdentidadGoogle;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Login con Google (AUTH-02): resuelve a qué cuenta corresponde un ID token ya verificado.
 *
 * <p>Deliberadamente SIN {@code @Transactional}: si el alta falla por la carrera de dos ingresos simultáneos, una
 * transacción abierta quedaría marcada para rollback y no se podría reintentar la búsqueda. Cada llamada al
 * repositorio confirma por su cuenta; los UNIQUE de {@code google_sub} y de {@code lower(email)} son la garantía.
 *
 * <p>Nunca se loguea el token, el sub ni el mail.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String MAIL_NO_CONFIRMADO = "Google no confirmó tu mail: usá tu mail y contraseña.";
    private static final String CUENTA_ADMIN = "Esta cuenta no puede ingresar con Google.";
    private static final String OTRO_GOOGLE = "Esta cuenta ya está vinculada a otra cuenta de Google.";

    private final GoogleIdTokenVerifier verificador;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;
    private final Clock clock;

    public AuthResponse entrar(String credential) {
        // 1. Un token inválido propaga BadCredentialsException sin tocar la base.
        IdentidadGoogle identidad = verificador.verificar(credential);

        // 2. D-06: solo se vincula o se crea con un mail que Google confirmó.
        String email = identidad.email() == null ? "" : identidad.email().trim().toLowerCase(Locale.ROOT);
        if (!identidad.emailVerificado() || email.isEmpty()) {
            throw new ReglaDeNegocioException(MAIL_NO_CONFIRMADO);
        }

        // 3. La identidad estable es el sub, nunca el mail.
        Optional<Usuario> porSub = usuarioRepository.findByGoogleSub(identidad.sub());
        if (porSub.isPresent()) {
            return authService.iniciarSesion(porSub.get());
        }

        // 4. Primera vez con este Google: unir a la cuenta del mismo mail o crear una nueva.
        Optional<Usuario> porMail = usuarioRepository.findByEmailIgnoreCase(email);
        Usuario usuario = porMail.isPresent()
                ? vincular(porMail.get(), identidad)
                : crear(identidad, email);

        // 6. El mismo AuthResponse que el login; informa los faltantes para llevar a Completá tus datos (D-07).
        return authService.iniciarSesion(usuario);
    }

    private Usuario vincular(Usuario cuenta, IdentidadGoogle identidad) {
        // Ningún camino alternativo llega a un admin sin una decisión explícita (D-23).
        if (cuenta.getRol() == Rol.ADMIN) {
            throw new ReglaDeNegocioException(CUENTA_ADMIN);
        }
        if (cuenta.getGoogleSub() != null && !cuenta.getGoogleSub().equals(identidad.sub())) {
            throw new ReglaDeNegocioException(OTRO_GOOGLE);
        }

        // D-15 (anti pre-hijacking): quien registró el mail antes que su dueño no conserva el acceso. La contraseña de
        // una cuenta con el mail sin confirmar se descarta y passwordCambiadaEn corta cualquier sesión que ya tuviera.
        // Una cuenta con el mail confirmado conserva su contraseña y puede usar los dos métodos (D-06).
        if (!cuenta.isEmailConfirmado() && cuenta.getPasswordHash() != null) {
            cuenta.setPasswordHash(null);
            cuenta.setPasswordCambiadaEn(LocalDateTime.now(clock));
        }

        cuenta.setGoogleSub(identidad.sub());
        cuenta.setEmailConfirmado(true); // Google confirmó el mail.
        if ((cuenta.getApellido() == null || cuenta.getApellido().isBlank()) && identidad.apellido() != null) {
            cuenta.setApellido(identidad.apellido());
        }
        return usuarioRepository.save(cuenta);
    }

    private Usuario crear(IdentidadGoogle identidad, String email) {
        Usuario nuevo = Usuario.builder()
                .nombre(nombreDe(identidad, email))
                .apellido(identidad.apellido())
                .email(email)
                .googleSub(identidad.sub())
                .emailConfirmado(true)
                .rol(Rol.COMPRADOR) // el ingreso con Google siempre crea compradores; los admins se cargan aparte
                .build(); // sin teléfono, sin DNI y sin contraseña (D-07, D-08)
        try {
            return usuarioRepository.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            // Dos ingresos simultáneos del mismo Google: el UNIQUE hizo perder a uno. Se reintenta una vez la
            // búsqueda por sub; si no aparece la cuenta, el conflicto era otro y se relanza.
            return usuarioRepository.findByGoogleSub(identidad.sub()).orElseThrow(() -> e);
        }
    }

    // El nombre es obligatorio en la cuenta: sin given_name ni name se usa la parte local del mail (editable después).
    private static String nombreDe(IdentidadGoogle identidad, String email) {
        if (identidad.nombre() != null && !identidad.nombre().isBlank()) {
            return identidad.nombre();
        }
        return email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
    }
}
