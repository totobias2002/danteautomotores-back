package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.Combustible;
import com.danteautomotores.enums.Condicion;
import com.danteautomotores.enums.Transmision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublicacionRequest {

    @NotNull
    private Long agenciaId;

    @NotBlank
    private String marca;

    @NotBlank
    private String modelo;

    @NotNull
    private Integer anio;

    @NotNull
    @Positive
    private BigDecimal precio;

    private String moneda;

    private Integer kilometraje;
    private Transmision transmision;
    private Combustible combustible;
    private String color;
    private Condicion condicion;
    private String descripcion;
}
