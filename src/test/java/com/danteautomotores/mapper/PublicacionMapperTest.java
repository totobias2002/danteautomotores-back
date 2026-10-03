package com.danteautomotores.mapper;

import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.enums.ZonaAgencia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PublicacionMapperTest {

    private static Publicacion auto(String precio, String precioAnterior, ZonaAgencia zona) {
        Agencia agencia = Agencia.builder().id(1L).nombre("Dante").slug("dante").zona(zona).build();
        return Publicacion.builder()
                .id(5L)
                .agencia(agencia)
                .marca("Toyota")
                .modelo("Corolla")
                .anio(2020)
                .precio(precio == null ? null : new BigDecimal(precio))
                .precioAnterior(precioAnterior == null ? null : new BigDecimal(precioAnterior))
                .tipoCarroceria(TipoCarroceria.SUV)
                .build();
    }

    @Test
    void sinPrecioAnteriorNoEsOferta() {
        assertThat(PublicacionMapper.esOferta(auto("100", null, null))).isFalse();
    }

    @Test
    void precioAnteriorIgualAlPrecioNoEsOferta() {
        assertThat(PublicacionMapper.esOferta(auto("100", "100", null))).isFalse();
    }

    @Test
    void precioAnteriorMenorAlPrecioNoEsOfertaNiMuestraUnDescuentoInexistente() {
        assertThat(PublicacionMapper.esOferta(auto("100", "90", null))).isFalse();
    }

    @Test
    void precioAnteriorMayorAlPrecioEsOferta() {
        assertThat(PublicacionMapper.esOferta(auto("100", "120", null))).isTrue();
    }

    @Test
    void laComparacionIgnoraLaEscala() {
        assertThat(PublicacionMapper.esOferta(auto("120", "120.00", null))).isFalse();
        assertThat(PublicacionMapper.esOferta(auto("119.99", "120", null))).isTrue();
    }

    @Test
    void sinPrecioNoEsOferta() {
        assertThat(PublicacionMapper.esOferta(auto(null, "120", null))).isFalse();
    }

    @Test
    void toResponseExponeTipoPrecioAnteriorOfertaYZonaDeLaAgencia() {
        PublicacionResponse respuesta = PublicacionMapper.toResponse(auto("100", "120", ZonaAgencia.ZONA_NORTE));

        assertThat(respuesta.getTipoCarroceria()).isEqualTo(TipoCarroceria.SUV);
        assertThat(respuesta.getPrecioAnterior()).isEqualByComparingTo("120");
        assertThat(respuesta.isOferta()).isTrue();
        assertThat(respuesta.getAgenciaZona()).isEqualTo(ZonaAgencia.ZONA_NORTE);
    }

    @Test
    void toResumenExponeTipoPrecioAnteriorOfertaYZonaDeLaAgencia() {
        PublicacionResumenResponse resumen = PublicacionMapper.toResumen(auto("100", "120", ZonaAgencia.ZONA_NORTE));

        assertThat(resumen.getTipoCarroceria()).isEqualTo(TipoCarroceria.SUV);
        assertThat(resumen.getPrecioAnterior()).isEqualByComparingTo("120");
        assertThat(resumen.isOferta()).isTrue();
        assertThat(resumen.getAgenciaZona()).isEqualTo(ZonaAgencia.ZONA_NORTE);
    }

    @Test
    void conAgenciaSinZonaElAutoNoTraeZona() {
        Publicacion publicacion = auto("100", "90", null);

        assertThat(PublicacionMapper.toResponse(publicacion).getAgenciaZona()).isNull();
        assertThat(PublicacionMapper.toResumen(publicacion).getAgenciaZona()).isNull();
        assertThat(PublicacionMapper.toResponse(publicacion).isOferta()).isFalse();
        assertThat(PublicacionMapper.toResumen(publicacion).isOferta()).isFalse();
    }
}
