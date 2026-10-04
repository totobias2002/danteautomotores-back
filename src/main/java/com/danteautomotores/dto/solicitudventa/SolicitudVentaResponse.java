package com.danteautomotores.dto.solicitudventa;

import com.danteautomotores.enums.EstadoSolicitudVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudVentaResponse {
    private Long id;
    private String marca;
    private String modelo;
    private Integer anio;
    private Integer kilometraje;
    private BigDecimal cotizacionMin;
    private BigDecimal cotizacionMax;
    private String nombreVendedor;
    private String telefonoVendedor;
    private String ciudad;
    private String descripcion;
    private EstadoSolicitudVenta estado;
    private LocalDateTime fecha;
}
