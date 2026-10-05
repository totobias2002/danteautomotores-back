package com.danteautomotores.mapper;

import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;

import java.util.List;

public class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario, List<DatoFaltante> faltantes) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .dni(usuario.getDni())
                .rol(usuario.getRol().name())
                .emailConfirmado(usuario.isEmailConfirmado())
                .tieneContrasena(usuario.getPasswordHash() != null)
                .tieneGoogle(usuario.getGoogleSub() != null)
                .cuentaVerificada(faltantes.isEmpty())
                .faltantes(faltantes)
                .build();
    }
}
