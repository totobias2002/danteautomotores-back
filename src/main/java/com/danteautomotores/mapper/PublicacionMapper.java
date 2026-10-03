package com.danteautomotores.mapper;

import com.danteautomotores.dto.publicacion.FotoResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.FotoPublicacion;
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
                .destacado(publicacion.isDestacado())
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .fotos(publicacion.getFotos().stream()
                        .sorted(Comparator.comparing(f -> f.getOrden() == null ? 0 : f.getOrden()))
                        .map(f -> FotoResponse.builder().id(f.getId()).url(f.getUrl()).orden(f.getOrden()).build())
                        .toList())
                .build();
    }

    // Versión liviana para los listados públicos. La portada es la foto de menor orden (mismo criterio que toResponse:
    // orden null cuenta como 0) o null si el auto no tiene fotos.
    public static PublicacionResumenResponse toResumen(Publicacion publicacion) {
        return PublicacionResumenResponse.builder()
                .id(publicacion.getId())
                .marca(publicacion.getMarca())
                .modelo(publicacion.getModelo())
                .anio(publicacion.getAnio())
                .kilometraje(publicacion.getKilometraje())
                .precio(publicacion.getPrecio())
                .moneda(publicacion.getMoneda())
                .estado(publicacion.getEstado())
                .destacado(publicacion.isDestacado())
                .transmision(publicacion.getTransmision())
                .color(publicacion.getColor())
                .agenciaId(publicacion.getAgencia().getId())
                .agenciaNombre(publicacion.getAgencia().getNombre())
                .agenciaSlug(publicacion.getAgencia().getSlug())
                .fotoPortada(publicacion.getFotos().stream()
                        .min(Comparator.comparing((FotoPublicacion f) -> f.getOrden() == null ? 0 : f.getOrden()))
                        .map(FotoPublicacion::getUrl)
                        .orElse(null))
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .build();
    }
}
