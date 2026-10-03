package com.danteautomotores.repository;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRol(Rol rol);
}
