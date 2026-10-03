package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.Combustible;
import com.danteautomotores.enums.Condicion;
import com.danteautomotores.enums.Transmision;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

// Los límites coinciden con las columnas de Publicacion (varchar(255), numeric(12,2)): si no se validan acá, un dato
// demasiado largo llega a la base y vuelve como un 409 engañoso en vez de un 400 con el campo señalado.
@Data
public class PublicacionRequest {

    @NotNull(message = "Elegí la agencia")
    private Long agenciaId;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 255, message = "La marca no puede superar los 255 caracteres")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 255, message = "El modelo no puede superar los 255 caracteres")
    private String modelo;

    @NotNull(message = "El año es obligatorio")
    @AnioDeModelo
    private Integer anio;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio tiene que ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 dígitos enteros y 2 decimales")
    private BigDecimal precio;

    @Pattern(regexp = "ARS|USD", message = "La moneda tiene que ser ARS o USD")
    private String moneda;

    @PositiveOrZero(message = "El kilometraje no puede ser negativo")
    private Integer kilometraje;

    private Transmision transmision;
    private Combustible combustible;

    @Size(max = 255, message = "El color no puede superar los 255 caracteres")
    private String color;

    private Condicion condicion;

    @Size(min = 10, max = 5000, message = "La descripción tiene que tener entre 10 y 5000 caracteres")
    private String descripcion;
}
