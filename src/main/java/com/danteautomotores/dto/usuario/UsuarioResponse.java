package com.danteautomotores.dto.usuario;

import com.danteautomotores.enums.DatoFaltante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Perfil de la propia cuenta. Es el único DTO que lleva el DNI y el teléfono (Ley 25.326): solo se devuelve a su
 * dueño y no se imprime en logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    @ToString.Exclude
    private String telefono;
    @ToString.Exclude
    private String dni;
    private String rol;
    private boolean emailConfirmado;
    private boolean tieneContrasena;
    private boolean tieneGoogle;
    private boolean cuentaVerificada;
    private List<DatoFaltante> faltantes;
}
