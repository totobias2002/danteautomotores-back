package com.danteautomotores.dto.solicitudventa;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SolicitudVentaRequest {

    @NotBlank
    private String marca;

    @NotBlank
    private String modelo;

    @NotNull
    private Integer anio;

    @Min(0)
    private Integer kilometraje;

    private BigDecimal cotizacionMin;

    private BigDecimal cotizacionMax;

    @NotBlank
    private String nombreVendedor;

    @NotBlank
    private String telefonoVendedor;

    private String ciudad;

    private String descripcion;
}
