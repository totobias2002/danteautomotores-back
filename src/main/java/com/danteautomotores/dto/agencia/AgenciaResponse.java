package com.danteautomotores.dto.agencia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenciaResponse {
    private Long id;
    private String nombre;
    private String slug;
    private String logo;
    private String descripcion;
    private String direccion;
    private String telefonoContacto;
    private String emailContacto;
    private LocalDateTime fechaAlta;
}
