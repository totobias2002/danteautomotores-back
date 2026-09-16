package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .rol(Rol.COMPRADOR) // el registro público siempre crea compradores; los admins se cargan aparte
                .build();

        usuarioRepository.save(usuario);

        return construirRespuesta(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        return construirRespuesta(usuario);
    }

    private AuthResponse construirRespuesta(Usuario usuario) {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .authorities("ROLE_" + usuario.getRol().name())
                        .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }
}
