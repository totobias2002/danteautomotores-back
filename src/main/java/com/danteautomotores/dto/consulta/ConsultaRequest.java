package com.danteautomotores.dto.consulta;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsultaRequest {

    @NotNull
    private Long publicacionId;

    @NotBlank
    private String nombreComprador;

    @NotBlank
    @Email
    private String emailComprador;

    private String telefonoComprador;

    @NotBlank
    private String mensaje;
}
