package com.danteautomotores.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * Entrada del registro público. No declara un campo de rol (la cuenta siempre es de comprador) ni uno de aceptación de
 * privacidad (D-20: va como leyenda en el formulario). Contraseña, teléfono y DNI quedan fuera del toString: la
 * Ley 25.326 pide no volcarlos a un log.
 */
@Data
public class RegistroRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    private String apellido;

    @NotBlank
    @Email
    @Size(max = 254)
    @ToString.Exclude
    private String email;

    // BCrypt solo considera los primeros 72 bytes: un tope mayor aceptaría contraseñas que se truncan en silencio.
    @NotBlank
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @ToString.Exclude
    private String password;

    @NotBlank
    @Size(max = 30)
    @ToString.Exclude
    private String telefono;

    @NotBlank
    @Size(max = 20)
    @ToString.Exclude
    private String dni;
}
