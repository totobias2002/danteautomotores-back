package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Transmision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ítem de los listados públicos del catálogo. Solo datos del auto y de su agencia: no trae la descripción, la lista
 * completa de fotos ni nada del admin que lo cargó (es una respuesta sin token).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionResumenResponse {
    private Long id;
    private String marca;
    private String modelo;
    private Integer anio;
    private Integer kilometraje;
    private BigDecimal precio;
    private String moneda;
    private EstadoPublicacion estado;
    private boolean destacado;
    private Transmision transmision;
    private String color;
    private Long agenciaId;
    private String agenciaNombre;
    private String agenciaSlug;
    private String fotoPortada;
    private LocalDateTime fechaPublicacion;
}
