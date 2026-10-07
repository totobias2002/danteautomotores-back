package com.danteautomotores.dto.conversacion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Solo declara el auto y el texto opcional. Tipo, estado y usuario nunca vienen del cliente: un cuerpo que los traiga
 * de más se ignora (T-04-05).
 */
@Data
public class ConversacionRequest {

    @NotNull(message = "Elegí el auto")
    private Long publicacionId;

    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String mensaje;
}
