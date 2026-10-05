package com.danteautomotores.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Pedido de cambio de contraseña. El mail queda fuera del toString (Ley 25.326). */
@Data
public class OlvideContrasenaRequest {

    @NotBlank
    @Email
    @Size(max = 254)
    @ToString.Exclude
    private String email;
}
