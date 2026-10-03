package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.PublicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lectura del catálogo público (sin token). Es una clase aparte de PublicacionService, que concentra las escrituras
 * del admin; acá solo se devuelven DTOs resumidos, sin datos de usuarios.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogoService {

    static final int LIMITE_DESTACADOS_POR_DEFECTO = 6;
    static final int LIMITE_DESTACADOS_MAXIMO = 12;

    private final PublicacionRepository publicacionRepository;

    public List<PublicacionResumenResponse> destacados(Integer limite) {
        // El límite nunca lo decide un tamaño arbitrario del cliente: se acota a 1..12.
        int tope = limite == null ? LIMITE_DESTACADOS_POR_DEFECTO
                : Math.min(Math.max(limite, 1), LIMITE_DESTACADOS_MAXIMO);
        // El mapeo va adentro de la transacción: open-in-view está apagado y agencia/fotos son lazy.
        return publicacionRepository.findDestacadosVisibles(EstadoPublicacion.VENDIDO, PageRequest.of(0, tope))
                .stream()
                .map(PublicacionMapper::toResumen)
                .toList();
    }
}
