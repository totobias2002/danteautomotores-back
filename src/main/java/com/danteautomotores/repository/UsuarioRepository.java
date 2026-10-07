package com.danteautomotores.repository;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRol(Rol rol);

    // El mail se compara sin distinguir mayúsculas (índice único sobre lower(email) en V5).
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);

    Optional<Usuario> findByGoogleSub(String googleSub);

    // Todas las cuentas de un rol: los avisos de mensaje nuevo van a cada cuenta admin.
    List<Usuario> findByRol(Rol rol);
}
