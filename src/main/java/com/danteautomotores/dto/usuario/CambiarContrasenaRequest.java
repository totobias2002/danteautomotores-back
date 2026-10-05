package com.danteautomotores.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Cambio de contraseña desde el perfil: exige la actual. Ambas quedan fuera del toString. */
@Data
public class CambiarContrasenaRequest {

    @NotBlank
    @Size(max = 200)
    @ToString.Exclude
    private String actual;

    // BCrypt solo considera los primeros 72 bytes: un tope mayor aceptaría contraseñas que se truncan en silencio.
    @NotBlank
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @ToString.Exclude
    private String nueva;
}
