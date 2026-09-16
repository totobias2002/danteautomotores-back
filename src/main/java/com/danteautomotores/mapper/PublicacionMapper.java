package com.danteautomotores.mapper;

import com.danteautomotores.dto.publicacion.FotoResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.entity.Publicacion;

import java.util.Comparator;

public class PublicacionMapper {

    private PublicacionMapper() {
    }

    public static PublicacionResponse toResponse(Publicacion publicacion) {
        return PublicacionResponse.builder()
                .id(publicacion.getId())
                .agenciaId(publicacion.getAgencia().getId())
                .agenciaNombre(publicacion.getAgencia().getNombre())
                .agenciaSlug(publicacion.getAgencia().getSlug())
                .marca(publicacion.getMarca())
                .modelo(publicacion.getModelo())
                .anio(publicacion.getAnio())
                .precio(publicacion.getPrecio())
                .moneda(publicacion.getMoneda())
                .kilometraje(publicacion.getKilometraje())
                .transmision(publicacion.getTransmision())
                .combustible(publicacion.getCombustible())
                .color(publicacion.getColor())
                .condicion(publicacion.getCondicion())
                .descripcion(publicacion.getDescripcion())
                .estado(publicacion.getEstado())
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .fotos(publicacion.getFotos().stream()
                        .sorted(Comparator.comparing(f -> f.getOrden() == null ? 0 : f.getOrden()))
                        .map(f -> FotoResponse.builder().id(f.getId()).url(f.getUrl()).orden(f.getOrden()).build())
                        .toList())
                .build();
    }
}
