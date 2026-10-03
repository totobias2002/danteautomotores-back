package com.danteautomotores.dto.publicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Opciones y rangos de los filtros del catálogo, calculados sobre los autos visibles. Los valores de enum viajan como
 * nombre (el front pone las etiquetas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetasResponse {
    private List<Conteo> marcas;
    private List<ConteoModelo> modelos;
    private List<Conteo> tipos;
    private List<Conteo> zonas;
    private List<Conteo> colores;
    private List<Conteo> transmisiones;
    private List<Conteo> estados;
    private Rango anio;
    private Rango kilometraje;
    // Null si no hay autos visibles.
    private RangoPrecio precio;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conteo {
        private String valor;
        private long cantidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConteoModelo {
        private String marca;
        private String valor;
        private long cantidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rango {
        private Integer min;
        private Integer max;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TramoPrecio {
        private BigDecimal desde;
        private BigDecimal hasta;
        private long cantidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangoPrecio {
        private BigDecimal min;
        private BigDecimal max;
        private List<TramoPrecio> histograma;
    }
}
