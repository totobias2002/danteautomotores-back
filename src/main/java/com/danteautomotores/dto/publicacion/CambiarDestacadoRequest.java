package com.danteautomotores.dto.publicacion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarDestacadoRequest {

    // Wrapper (no boolean primitivo) para que un body sin el campo falle la validación en vez de asumir false.
    @NotNull
    private Boolean destacado;
}
