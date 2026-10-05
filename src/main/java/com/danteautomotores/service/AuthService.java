package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.CuentaUserDetails;
import com.danteautomotores.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificacionCuenta verificacionCuenta;

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ReglaDeNegocioException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .rol(Rol.COMPRADOR) // el registro público siempre crea compradores; los admins se cargan aparte
                .build();

        usuarioRepository.save(usuario);

        return iniciarSesion(usuario);
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
