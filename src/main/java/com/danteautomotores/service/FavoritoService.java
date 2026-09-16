package com.danteautomotores.service;

import com.danteautomotores.dto.favorito.FavoritoResponse;
import com.danteautomotores.entity.Favorito;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.FavoritoMapper;
import com.danteautomotores.repository.FavoritoRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;

    public List<FavoritoResponse> listar() {
        Usuario usuario = obtenerUsuarioAutenticado();
        return favoritoRepository.findByUsuarioId(usuario.getId()).stream()
                .map(FavoritoMapper::toResponse)
                .toList();
    }

    public FavoritoResponse agregar(Long publicacionId) {
        Usuario usuario = obtenerUsuarioAutenticado();

        if (favoritoRepository.existsByUsuarioIdAndPublicacionId(usuario.getId(), publicacionId)) {
            throw new IllegalArgumentException("Esta publicación ya está en tus favoritos");
        }

        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + publicacionId));

        Favorito favorito = Favorito.builder()
                .usuario(usuario)
                .publicacion(publicacion)
                .build();

        favoritoRepository.save(favorito);
        return FavoritoMapper.toResponse(favorito);
    }

    public void quitar(Long publicacionId) {
        Usuario usuario = obtenerUsuarioAutenticado();
        Favorito favorito = favoritoRepository.findByUsuarioIdAndPublicacionId(usuario.getId(), publicacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Esa publicación no está en tus favoritos"));
        favoritoRepository.delete(favorito);
    }

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityUtils.obtenerEmailAutenticado();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
