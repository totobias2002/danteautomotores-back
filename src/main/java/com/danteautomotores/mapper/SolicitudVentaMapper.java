package com.danteautomotores.mapper;

import com.danteautomotores.dto.solicitudventa.SolicitudVentaResponse;
import com.danteautomotores.entity.SolicitudVenta;

public class SolicitudVentaMapper {

    private SolicitudVentaMapper() {
    }

    public static SolicitudVentaResponse toResponse(SolicitudVenta solicitud) {
        return SolicitudVentaResponse.builder()
                .id(solicitud.getId())
                .marca(solicitud.getMarca())
                .modelo(solicitud.getModelo())
                .anio(solicitud.getAnio())
                .kilometraje(solicitud.getKilometraje())
                .cotizacionMin(solicitud.getCotizacionMin())
                .cotizacionMax(solicitud.getCotizacionMax())
                .nombreVendedor(solicitud.getNombreVendedor())
                .telefonoVendedor(solicitud.getTelefonoVendedor())
                .ciudad(solicitud.getCiudad())
                .descripcion(solicitud.getDescripcion())
                .estado(solicitud.getEstado())
                .fecha(solicitud.getFecha())
                .build();
    }
}
