package com.danteautomotores.dto.agencia;

import com.danteautomotores.enums.ZonaAgencia;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Los límites coinciden con las columnas de Agencia (varchar(255)): si no se validan acá, un dato demasiado largo llega
// a la base y vuelve como un 409 engañoso en vez de un 400 con el campo señalado. La descripción es TEXT (sin límite).
@Data
public class AgenciaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String nombre;

    @Size(max = 255, message = "El logo no puede superar los 255 caracteres")
    private String logo;

    private String descripcion;

    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String direccion;

    @Size(max = 255, message = "El teléfono no puede superar los 255 caracteres")
    private String telefonoContacto;

    // Opcional (D-02): sin zona, la agencia y sus autos quedan en "sin especificar".
    private ZonaAgencia zona;

    // El email de contacto se publica en GET /api/agencias y en la página de la agencia.
    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El email de contacto no tiene un formato válido")
    @Size(max = 255, message = "El email de contacto no puede superar los 255 caracteres")
    private String emailContacto;
}
