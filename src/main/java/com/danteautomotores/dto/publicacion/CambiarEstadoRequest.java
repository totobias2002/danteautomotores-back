package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.EstadoPublicacion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoRequest {

    @NotNull
    private EstadoPublicacion estado;
}
