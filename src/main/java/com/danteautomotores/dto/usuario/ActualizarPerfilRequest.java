package com.danteautomotores.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * Entrada de "Completá tus datos" y del perfil. No declara mail, rol ni ningún dato de identidad de la cuenta: el mail
 * no se edita en esta fase y Jackson ignora lo que no existe (sin mass assignment). El DNI es opcional en la entrada
 * porque una cuenta que ya lo tiene cargado no lo vuelve a mandar; el service decide cuándo es obligatorio.
 */
@Data
public class ActualizarPerfilRequest {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    private String apellido;

    @NotBlank
    @Size(max = 30)
    @ToString.Exclude
    private String telefono;

    @Size(max = 20)
    @ToString.Exclude
    private String dni;
}
