package com.danteautomotores.mapper;

import com.danteautomotores.dto.publicacion.FotoResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.enums.ZonaAgencia;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    void ambosMappersExponenLaFechaDeVenta() {
        Publicacion publicacion = auto("100", null, null);
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 1, 12, 0);
        publicacion.setFechaVendido(fecha);

        assertThat(PublicacionMapper.toResponse(publicacion).getFechaVendido()).isEqualTo(fecha);
        assertThat(PublicacionMapper.toResumen(publicacion).getFechaVendido()).isEqualTo(fecha);
    }

    private static FotoPublicacion foto(Long id, String url, Integer orden) {
        return FotoPublicacion.builder().id(id).url(url).orden(orden).build();
    }

    @Test
    void laPortadaDelResumenEsLaPrimeraFotoDelDetalleAunqueEmpatenEnOrden() {
        List<List<FotoPublicacion>> ordenesFisicos = List.of(
                List.of(foto(7L, "siete", 0), foto(3L, "tres", null), foto(9L, "nueve", 1)),
                List.of(foto(9L, "nueve", 1), foto(3L, "tres", null), foto(7L, "siete", 0)));

        for (List<FotoPublicacion> fotos : ordenesFisicos) {
            Publicacion publicacion = auto("100", null, null);
            publicacion.getFotos().addAll(fotos);

            PublicacionResponse detalle = PublicacionMapper.toResponse(publicacion);
            PublicacionResumenResponse resumen = PublicacionMapper.toResumen(publicacion);

            assertThat(detalle.getFotos()).extracting(FotoResponse::getId).containsExactly(3L, 7L, 9L);
            assertThat(resumen.getFotoPortada()).isEqualTo("tres");
            assertThat(resumen.getFotoPortada()).isEqualTo(detalle.getFotos().get(0).getUrl());
        }
    }

    @Test
    void elOrdenExplicitoGanaAlId() {
        Publicacion publicacion = auto("100", null, null);
        publicacion.getFotos().addAll(List.of(foto(1L, "uno", 2), foto(2L, "dos", 0)));

        assertThat(PublicacionMapper.toResumen(publicacion).getFotoPortada()).isEqualTo("dos");
        assertThat(PublicacionMapper.toResponse(publicacion).getFotos())
                .extracting(FotoResponse::getId).containsExactly(2L, 1L);
    }

    @Test
    void unaFotoSinIdQuedaDespuesDeLasQueTienenId() {
        Publicacion publicacion = auto("100", null, null);
        publicacion.getFotos().addAll(List.of(foto(null, "nueva", 0), foto(4L, "cuatro", 0)));

        assertThat(PublicacionMapper.toResumen(publicacion).getFotoPortada()).isEqualTo("cuatro");
        assertThat(PublicacionMapper.toResponse(publicacion).getFotos())
                .extracting(FotoResponse::getId).containsExactly(4L, null);
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
