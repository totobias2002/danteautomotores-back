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

    // Única regla de orden de las fotos y, por lo tanto, de la portada (IN-04): la card, el detalle y el resecuenciado
    // al borrar usan este comparador. Orden ascendente con null contado como 0 (fotos viejas) y desempate por id, con
    // los ids null (todavía sin persistir) al final. Sin el desempate, dos fotos empatadas se resolvían según el orden
    // en que la base devolvía las filas y la card podía mostrar una portada distinta de la primera foto del detalle.
    public static final Comparator<FotoPublicacion> ORDEN_DE_FOTOS = Comparator
            .comparing((FotoPublicacion f) -> f.getOrden() == null ? 0 : f.getOrden())
            .thenComparing(FotoPublicacion::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    // Única regla de oferta (D-03), de la que dependen la card, el detalle y el filtro: hay oferta solo si el precio
    // anterior es mayor que el actual. compareTo y no equals: 120.00 y 120 son el mismo precio.
    public static boolean esOferta(Publicacion publicacion) {
        return publicacion.getPrecioAnterior() != null
                && publicacion.getPrecio() != null
                && publicacion.getPrecioAnterior().compareTo(publicacion.getPrecio()) > 0;
    }

    public static PublicacionResponse toResponse(Publicacion publicacion) {
        return PublicacionResponse.builder()
                .id(publicacion.getId())
                .agenciaId(publicacion.getAgencia().getId())
                .agenciaNombre(publicacion.getAgencia().getNombre())
                .agenciaSlug(publicacion.getAgencia().getSlug())
                .tipoCarroceria(publicacion.getTipoCarroceria())
                .precioAnterior(publicacion.getPrecioAnterior())
                .oferta(esOferta(publicacion))
                .agenciaZona(publicacion.getAgencia().getZona())
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
                .fechaVendido(publicacion.getFechaVendido())
                .fotos(publicacion.getFotos().stream()
                        .sorted(ORDEN_DE_FOTOS)
                        .map(f -> FotoResponse.builder().id(f.getId()).url(f.getUrl()).orden(f.getOrden()).build())
                        .toList())
                .build();
    }

    // Versión liviana para los listados públicos. La portada es la primera foto según ORDEN_DE_FOTOS, la misma que
    // encabeza fotos en el detalle (toResponse), o null si el auto no tiene fotos.
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
                .tipoCarroceria(publicacion.getTipoCarroceria())
                .precioAnterior(publicacion.getPrecioAnterior())
                .oferta(esOferta(publicacion))
                .agenciaZona(publicacion.getAgencia().getZona())
                .fotoPortada(publicacion.getFotos().stream()
                        .min(ORDEN_DE_FOTOS)
                        .map(FotoPublicacion::getUrl)
                        .orElse(null))
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .fechaVendido(publicacion.getFechaVendido())
                .build();
    }
}
