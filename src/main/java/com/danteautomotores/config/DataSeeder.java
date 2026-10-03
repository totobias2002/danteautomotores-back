package com.danteautomotores.config;

import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Siembra, al arrancar, la agencia inicial y la cuenta admin a partir de variables de entorno.
 * Ningún endpoint crea ni promueve admins: esta es la única vía.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private static final int PASSWORD_MIN = 8; // mismo criterio que RegistroRequest

    private final UsuarioRepository usuarioRepository;
    private final AgenciaRepository agenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.nombre:}")
    private String adminNombre;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        sembrarAgencia();
        sembrarAdmin();
    }

    // Solo si no hay ninguna agencia; el admin completa el resto desde el panel.
    // Nunca modifica ni borra agencias existentes.
    private void sembrarAgencia() {
        if (agenciaRepository.count() == 0) {
            agenciaRepository.save(Agencia.builder()
                    .nombre("Dante Automotores")
                    .slug("dante-automotores")
                    .build());
            log.info("Agencia inicial creada: Dante Automotores");
        }
    }

    private void sembrarAdmin() {
        // Si ya hay un admin no se sincroniza nada: cambiar las variables después no
        // modifica la contraseña ni el nombre de la cuenta existente.
        if (usuarioRepository.existsByRol(Rol.ADMIN)) {
            log.info("Ya existe una cuenta admin; el seed no la modifica");
            return;
        }
        // El login busca el email exacto: un espacio o una mayúscula de más en la variable dejaría un admin que no puede entrar.
        String email = adminEmail == null ? "" : adminEmail.trim().toLowerCase(Locale.ROOT);
        if (estaEnBlanco(email) || estaEnBlanco(adminPassword) || estaEnBlanco(adminNombre)) {
            fallarOAvisar("Faltan las variables ADMIN_EMAIL, ADMIN_PASSWORD o ADMIN_NOMBRE y no existe ninguna cuenta admin");
            return;
        }
        if (!email.contains("@")) {
            fallarOAvisar("ADMIN_EMAIL no es un email válido");
            return;
        }
        if (adminPassword.length() < PASSWORD_MIN) {
            fallarOAvisar("ADMIN_PASSWORD debe tener al menos " + PASSWORD_MIN + " caracteres");
            return;
        }
        // Nunca se promueve una cuenta existente (por ejemplo, un comprador) a admin.
        if (usuarioRepository.existsByEmail(email)) {
            fallarOAvisar("ADMIN_EMAIL ya pertenece a una cuenta que no es admin; usá otro email");
            return;
        }

        usuarioRepository.save(Usuario.builder()
                .nombre(adminNombre)
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(Rol.ADMIN)
                .build());
        log.info("Cuenta admin creada para {} (ingresá con ese email)", email);
    }

    // Fuera del modo desarrollo (cualquier perfil que no sea dev/local/test) abortar el arranque es mejor que dejar
    // la app sin admin; el criterio es el mismo que el de SecretosGuard.
    private void fallarOAvisar(String mensaje) {
        if (!EntornoDeDesarrollo.esDesarrollo(environment)) {
            throw new IllegalStateException(mensaje);
        }
        log.warn("{} (fuera del modo desarrollo, es decir con cualquier perfil que no sea dev/local/test, el arranque se aborta)",
                mensaje);
    }

    private boolean estaEnBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
