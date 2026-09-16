package com.danteautomotores.dto.agencia;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgenciaRequest {

    @NotBlank
    private String nombre;

    private String logo;
    private String descripcion;
    private String direccion;
    private String telefonoContacto;

    @NotBlank
    private String emailContacto;
}
