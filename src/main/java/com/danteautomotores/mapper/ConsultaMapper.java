package com.danteautomotores.mapper;

import com.danteautomotores.dto.consulta.ConsultaResponse;
import com.danteautomotores.entity.Consulta;

public class ConsultaMapper {

    private ConsultaMapper() {
    }

    public static ConsultaResponse toResponse(Consulta consulta) {
        return ConsultaResponse.builder()
                .id(consulta.getId())
                .publicacionId(consulta.getPublicacion().getId())
                .nombreComprador(consulta.getNombreComprador())
                .emailComprador(consulta.getEmailComprador())
                .telefonoComprador(consulta.getTelefonoComprador())
                .mensaje(consulta.getMensaje())
                .fecha(consulta.getFecha())
                .build();
    }
}
