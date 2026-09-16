package com.danteautomotores.mapper;

import com.danteautomotores.dto.agencia.AgenciaResponse;
import com.danteautomotores.entity.Agencia;

public class AgenciaMapper {

    private AgenciaMapper() {
    }

    public static AgenciaResponse toResponse(Agencia agencia) {
        return AgenciaResponse.builder()
                .id(agencia.getId())
                .nombre(agencia.getNombre())
                .slug(agencia.getSlug())
                .logo(agencia.getLogo())
                .descripcion(agencia.getDescripcion())
                .direccion(agencia.getDireccion())
                .telefonoContacto(agencia.getTelefonoContacto())
                .emailContacto(agencia.getEmailContacto())
                .fechaAlta(agencia.getFechaAlta())
                .build();
    }
}
