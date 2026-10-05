package com.danteautomotores.dto.consulta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Nombre, mail y teléfono ya no viajan en el pedido: salen de la cuenta autenticada (D-11). Un front viejo que los
 * siga mandando no rompe, porque Jackson ignora las propiedades desconocidas, y esos valores se descartan.
 */
@Data
public class ConsultaRequest {

    @NotNull
    private Long publicacionId;

    @NotBlank
    @Size(max = 2000)
    private String mensaje;
}
