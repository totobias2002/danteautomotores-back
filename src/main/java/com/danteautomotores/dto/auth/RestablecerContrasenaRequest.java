package com.danteautomotores.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Token del link y contraseña nueva. Ambos fuera del toString. */
@Data
public class RestablecerContrasenaRequest {

    @NotBlank
    @Size(max = 200)
    @ToString.Exclude
    private String token;

    // BCrypt solo considera los primeros 72 bytes: un tope mayor aceptaría contraseñas que se truncan en silencio.
    @NotBlank
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @ToString.Exclude
    private String password;
}
