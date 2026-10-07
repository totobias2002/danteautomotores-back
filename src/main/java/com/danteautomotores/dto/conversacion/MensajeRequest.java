package com.danteautomotores.dto.conversacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Solo el texto: el autor siempre es la cuenta del token y la conversación viene en la URL. */
@Data
public class MensajeRequest {

    @NotBlank(message = "Escribí un mensaje")
    @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
    private String texto;
}
