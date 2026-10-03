package com.danteautomotores.dto.agencia;

import com.danteautomotores.enums.ZonaAgencia;
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

    // Opcional (D-02): sin zona, la agencia y sus autos quedan en "sin especificar".
    private ZonaAgencia zona;

    @NotBlank
    private String emailContacto;
}
