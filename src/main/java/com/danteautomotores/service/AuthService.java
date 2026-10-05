package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.CuentaUserDetails;
import com.danteautomotores.security.JwtService;
import com.danteautomotores.service.identidad.NormalizadorDeContacto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificacionCuenta verificacionCuenta;
    private final TokenCuentaService tokenCuentaService;
    private final NotificacionesService notificacionesService;

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";
    private static final String MENSAJE_EMAIL_DUPLICADO = "Ya existe una cuenta con ese email";
    private static final int MAX_BYTES_CONTRASENA = 72;
    private static final String RESTRICCION_DNI_UNICO = "uk_usuarios_dni";
    // El índice de V5 sobre lower(email) y el UNIQUE original sobre email: según cuál se evalúe primero, Postgres informa uno u otro.
    private static final List<String> RESTRICCIONES_EMAIL_UNICO =
            List.of("uk_usuarios_email_lower", "ukkfsp0s1tflm1cwlj8idhqsad0");

    /**
     * Registro público con identidad completa (AUTH-01). No es @Transactional a propósito: cada llamada confirma por su
     * cuenta, así la cuenta y el token ya están guardados cuando el mail asíncrono sale con el link.
     */
    public AuthResponse registrar(RegistroRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // D-17: se mantiene el mensaje que revela que la cuenta existe. Es un compromiso de UX consciente frente a la
        // recomendación de OWASP de un mensaje genérico; queda como deuda de seguridad, mitigada con el límite de
        // intentos por IP del endpoint y con la confirmación del mail.
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaDeNegocioException(MENSAJE_EMAIL_DUPLICADO);
        }
        verificarLargoDeContrasena(request.getPassword());

        String telefono = NormalizadorDeContacto.normalizarCelular(request.getTelefono());
        String dni = NormalizadorDeContacto.normalizarDni(request.getDni());
        // D-04: se dice que el DNI ya está registrado, nunca de quién es.
        if (usuarioRepository.existsByDni(dni)) {
            throw new ReglaDeNegocioException(UsuarioService.MENSAJE_DNI_DUPLICADO);
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(telefono)
                .dni(dni)
                .emailConfirmado(false)
                .rol(Rol.COMPRADOR) // el registro público siempre crea compradores; los admins se cargan aparte
                .build();

        guardar(usuario);
        enviarConfirmacionDeEmail(usuario);

        return iniciarSesion(usuario);
    }

    /** BCrypt solo mira los primeros 72 bytes: más que eso se truncaría en silencio (acentos y símbolos ocupan más de 1). */
    private static void verificarLargoDeContrasena(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            throw new ReglaDeNegocioException(
                    "La contraseña es demasiado larga: no puede superar los 72 bytes (los acentos y símbolos ocupan más de uno).");
        }
    }

    private void guardar(Usuario usuario) {
        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Los chequeos previos no son atómicos: ante dos registros simultáneos manda el UNIQUE de la base.
            String restriccion = nombreDeRestriccion(e);
            if (RESTRICCION_DNI_UNICO.equalsIgnoreCase(restriccion)) {
                throw new ReglaDeNegocioException(UsuarioService.MENSAJE_DNI_DUPLICADO);
            }
            if (RESTRICCIONES_EMAIL_UNICO.stream().anyMatch(r -> r.equalsIgnoreCase(restriccion))) {
                throw new ReglaDeNegocioException(MENSAJE_EMAIL_DUPLICADO);
            }
            throw e;
        }
    }

    /** Si el token o el mail fallan el alta no falla: el usuario puede reenviar el mail (D-02, D-21). */
    private void enviarConfirmacionDeEmail(Usuario usuario) {
        try {
            String token = tokenCuentaService.emitir(usuario.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
            notificacionesService.enviarConfirmacionEmail(usuario, token);
        } catch (RuntimeException e) {
            // Solo la clase de la excepción: su mensaje podría traer el mail, el DNI o el teléfono (Ley 25.326).
            log.warn("No se pudo emitir o encolar el mail de confirmación del registro ({})", e.getClass().getSimpleName());
        }
    }

    private static String nombreDeRestriccion(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                return violacion.getConstraintName();
            }
        }
        return null;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Optional<Usuario> cuenta = usuarioRepository.findByEmailIgnoreCase(email);

        // Una cuenta sin contraseña (solo Google) recibe el mismo 401 genérico que una contraseña incorrecta:
        // no se llama a BCrypt ni se revela que la cuenta existe o que es de Google.
        if (cuenta.isPresent() && cuenta.get().getPasswordHash() == null) {
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        Usuario usuario = cuenta.orElseThrow(() -> new BadCredentialsException(CREDENCIALES_INVALIDAS));

        return iniciarSesion(usuario);
    }

    /**
     * Único punto por el que se emite una sesión: lo usan el registro, el login, Google y el cambio de contraseña.
     */
    public AuthResponse iniciarSesion(Usuario usuario) {
        String token = jwtService.generateToken(CuentaUserDetails.de(usuario));

        List<DatoFaltante> faltantes = verificacionCuenta.faltantes(usuario);

        // Nunca DNI ni teléfono: solo qué datos faltan.
        return AuthResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .emailConfirmado(usuario.isEmailConfirmado())
                .cuentaVerificada(faltantes.isEmpty())
                .faltantes(faltantes)
                .build();
    }
}
