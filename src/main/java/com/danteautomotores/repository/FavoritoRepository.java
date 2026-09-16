package com.danteautomotores.repository;

import com.danteautomotores.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuarioId(Long usuarioId);
    Optional<Favorito> findByUsuarioIdAndPublicacionId(Long usuarioId, Long publicacionId);
    boolean existsByUsuarioIdAndPublicacionId(Long usuarioId, Long publicacionId);
}
