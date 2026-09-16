package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.Combustible;
import com.danteautomotores.enums.Condicion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Transmision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionResponse {
    private Long id;
    private Long agenciaId;
    private String agenciaNombre;
    private String agenciaSlug;
    private String marca;
    private String modelo;
    private Integer anio;
    private BigDecimal precio;
    private String moneda;
    private Integer kilometraje;
    private Transmision transmision;
    private Combustible combustible;
    private String color;
    private Condicion condicion;
    private String descripcion;
    private EstadoPublicacion estado;
    private LocalDateTime fechaPublicacion;
    private List<FotoResponse> fotos;
}
