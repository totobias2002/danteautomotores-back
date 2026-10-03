package com.danteautomotores.dto.publicacion;

import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.enums.Transmision;
import com.danteautomotores.enums.ZonaAgencia;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parámetros de GET /api/publicaciones. Los nombres son los mismos que usa la URL del front. A propósito no hay
 * tamaño de página ni orden por propiedad: el tamaño es fijo en el servidor y el orden sale de un enum.
 */
@Data
@NoArgsConstructor
public class FiltrosCatalogo {
    private Integer pagina;
    private String orden;
    private String busqueda;
    private List<String> marca;
    private List<String> modelo;
    private List<String> color;
    private List<Transmision> transmision;
    private List<TipoCarroceria> tipo;
    private List<ZonaAgencia> zona;
    private List<EstadoPublicacion> estado;
    private Integer anioMin;
    private Integer anioMax;
    private Integer kmMax;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean ofertas;
    private Long agenciaId;
}
