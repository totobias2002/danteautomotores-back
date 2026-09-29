package com.danteautomotores.dto.solicitudventa;

import com.danteautomotores.enums.EstadoSolicitudVenta;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoSolicitudRequest {

    @NotNull
    private EstadoSolicitudVenta estado;
}
