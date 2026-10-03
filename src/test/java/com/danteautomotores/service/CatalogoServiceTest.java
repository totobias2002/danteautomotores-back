package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.FacetasResponse.TramoPrecio;
import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.OrdenCatalogo;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogoServiceTest {

    private PublicacionRepository repositorio;
    private CatalogoService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(PublicacionRepository.class);
        servicio = new CatalogoService(repositorio, Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void paginaNulaCeroONegativaVaALaPrimera() {
        for (Integer cruda : new Integer[]{null, 0, -3}) {
            FiltrosCatalogo f = new FiltrosCatalogo();
            f.setPagina(cruda);
            assertThat(CatalogoService.normalizar(f).getPagina()).as("pagina " + cruda).isEqualTo(1);
        }
        FiltrosCatalogo valida = new FiltrosCatalogo();
        valida.setPagina(7);
        assertThat(CatalogoService.normalizar(valida).getPagina()).isEqualTo(7);
    }

    @Test
    void unaPaginaMasAllaDelTopeVaALaPrimeraYNuncaRompeElOffset() {
        for (int cruda : new int[]{CatalogoService.MAX_PAGINA + 1, 90_000_000, Integer.MAX_VALUE}) {
            FiltrosCatalogo f = new FiltrosCatalogo();
            f.setPagina(cruda);
            assertThat(CatalogoService.normalizar(f).getPagina()).as("pagina " + cruda).isEqualTo(1);
        }
        FiltrosCatalogo tope = new FiltrosCatalogo();
        tope.setPagina(CatalogoService.MAX_PAGINA);
        assertThat(CatalogoService.normalizar(tope).getPagina()).isEqualTo(CatalogoService.MAX_PAGINA);
    }

    @Test
    void buscarConUnaPaginaGigantePideLaPrimeraEnVezDeUnOffsetFueraDeInt() {
        Page<Publicacion> vacia = new PageImpl<>(List.of(), PageRequest.of(0, 24), 0);
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(vacia);
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setPagina(90_000_000);

        servicio.buscar(f);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getOffset()).isLessThanOrEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void elOrdenAceptaMayusculasOMinusculasYLoDesconocidoVaARelevancia() {
        assertThat(OrdenCatalogo.desde("PRECIO_ASC")).isEqualTo(OrdenCatalogo.PRECIO_ASC);
        assertThat(OrdenCatalogo.desde("precio_asc")).isEqualTo(OrdenCatalogo.PRECIO_ASC);
        assertThat(OrdenCatalogo.desde("xyz")).isEqualTo(OrdenCatalogo.RELEVANCIA);
        assertThat(OrdenCatalogo.desde(null)).isEqualTo(OrdenCatalogo.RELEVANCIA);
        assertThat(OrdenCatalogo.desde("")).isEqualTo(OrdenCatalogo.RELEVANCIA);

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setOrden("precio_asc");
        assertThat(CatalogoService.normalizar(f).getOrden()).isEqualTo("PRECIO_ASC");
        f.setOrden("xyz");
        assertThat(CatalogoService.normalizar(f).getOrden()).isEqualTo("RELEVANCIA");
        f.setOrden(null);
        assertThat(CatalogoService.normalizar(f).getOrden()).isEqualTo("RELEVANCIA");
    }

    @Test
    void unaListaDeMasDeVeinteValoresSeRecortaALosPrimerosVeinte() {
        List<String> marcas = IntStream.rangeClosed(1, 25).mapToObj(i -> "Marca" + i).toList();
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setMarca(marcas);

        List<String> normalizadas = CatalogoService.normalizar(f).getMarca();

        assertThat(normalizadas).hasSize(20).containsExactlyElementsOf(marcas.subList(0, 20));
    }

    @Test
    void laBusquedaDeMasDeSesentaCaracteresSeRecortaYLaEnBlancoSeDescarta() {
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setBusqueda("a".repeat(80));
        assertThat(CatalogoService.normalizar(f).getBusqueda()).hasSize(60);

        f.setBusqueda("   ");
        assertThat(CatalogoService.normalizar(f).getBusqueda()).isNull();
        f.setBusqueda(null);
        assertThat(CatalogoService.normalizar(f).getBusqueda()).isNull();
    }

    @Test
    void lasListasDeTextoDescartanValoresEnBlancoYRecortanEspacios() {
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setColor(new ArrayList<>(java.util.Arrays.asList(" Blanco ", "", "  ", null, "Negro")));
        f.setTipo(List.of(TipoCarroceria.SEDAN));

        FiltrosCatalogo n = CatalogoService.normalizar(f);

        assertThat(n.getColor()).containsExactly("Blanco", "Negro");
        assertThat(n.getTipo()).containsExactly(TipoCarroceria.SEDAN);
        assertThat(n.getModelo()).isNull();
    }

    @Test
    void normalizarNoModificaLaEntrada() {
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setPagina(-1);
        f.setOrden("xyz");
        f.setBusqueda("  corolla  ");

        CatalogoService.normalizar(f);

        assertThat(f.getPagina()).isEqualTo(-1);
        assertThat(f.getOrden()).isEqualTo("xyz");
        assertThat(f.getBusqueda()).isEqualTo("  corolla  ");
    }

    @Test
    void buscarPideLaPaginaBase0DeTamanio24SinSortDelCliente() {
        Page<Publicacion> vacia = new PageImpl<>(List.of(), PageRequest.of(2, 24), 0);
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(vacia);
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setPagina(3);

        servicio.buscar(f);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(24);
        assertThat(captor.getValue().getSort().isSorted()).isFalse();
    }

    @Test
    void buscarDevuelveElContratoDePaginaConBase1() {
        Page<Publicacion> pagina = new PageImpl<>(List.of(), PageRequest.of(1, 24), 30);
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        var respuesta = servicio.buscar(new FiltrosCatalogo());

        assertThat(respuesta.pagina()).isEqualTo(2);
        assertThat(respuesta.tamanio()).isEqualTo(24);
        assertThat(respuesta.totalElementos()).isEqualTo(30);
        assertThat(respuesta.totalPaginas()).isEqualTo(2);
    }

    // ---- Histograma de precios ----

    private static List<BigDecimal> precios(String... valores) {
        return java.util.Arrays.stream(valores).map(BigDecimal::new).toList();
    }

    @Test
    void elHistogramaTiene16TramosDeIgualAnchoYElUltimoIncluyeElMaximo() {
        List<TramoPrecio> tramos = CatalogoService.histograma(precios("10", "20", "30", "40"));

        assertThat(tramos).hasSize(16);
        assertThat(tramos.get(0).getDesde()).isEqualByComparingTo("10");
        assertThat(tramos.get(1).getDesde()).isEqualByComparingTo("11.88"); // 10 + 30/16 = 11,875 a 2 decimales
        assertThat(tramos.get(15).getHasta()).isEqualByComparingTo("40");
        assertThat(tramos.stream().mapToLong(TramoPrecio::getCantidad).sum()).isEqualTo(4);
        assertThat(tramos.get(0).getCantidad()).isEqualTo(1);   // el 10 en el primer tramo
        assertThat(tramos.get(15).getCantidad()).isEqualTo(1);  // el 40 (el máximo) en el último
    }

    @Test
    void conTodosLosPreciosIgualesHayUnSoloTramoConTodos() {
        List<TramoPrecio> tramos = CatalogoService.histograma(precios("500", "500", "500"));

        assertThat(tramos).hasSize(1);
        assertThat(tramos.get(0).getCantidad()).isEqualTo(3);
        assertThat(tramos.get(0).getDesde()).isEqualByComparingTo("500");
        assertThat(tramos.get(0).getHasta()).isEqualByComparingTo("500");
    }

    @Test
    void sinPreciosElHistogramaEstaVacio() {
        assertThat(CatalogoService.histograma(List.of())).isEmpty();
    }

    @Test
    void laSumaDelHistogramaEsElTotalDePreciosAunConValoresEnLosBordes() {
        List<BigDecimal> muchos = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            muchos.add(new BigDecimal(1000 + i * 37));
        }

        List<TramoPrecio> tramos = CatalogoService.histograma(muchos);

        assertThat(tramos).hasSize(16);
        assertThat(tramos.stream().mapToLong(TramoPrecio::getCantidad).sum()).isEqualTo(101);
    }

    @Test
    void sinAutosLasFacetasVienenVaciasYSinPrecio() {
        when(repositorio.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());

        var facetas = servicio.facetas(null);

        assertThat(facetas.getPrecio()).isNull();
        assertThat(facetas.getMarcas()).isEmpty();
        assertThat(facetas.getAnio().getMin()).isNull();
    }

    private Pageable similaresConLimite(Integer limite) {
        when(repositorio.findById(5L)).thenReturn(Optional.of(
                Publicacion.builder().id(5L).marca("Toyota").modelo("Corolla").anio(2020).precio(new BigDecimal("1000")).build()));
        when(repositorio.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.similares(5L, limite);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        return captor.getValue();
    }

    @Test
    void similaresSinLimitePideCuatro() {
        assertThat(similaresConLimite(null).getPageSize()).isEqualTo(4);
    }

    @Test
    void similaresConLimiteCeroPideUno() {
        assertThat(similaresConLimite(0).getPageSize()).isEqualTo(1);
    }

    @Test
    void similaresConLimiteEnormeSeAcotaAOcho() {
        assertThat(similaresConLimite(20).getPageSize()).isEqualTo(8);
    }

    @Test
    void similaresDeUnIdInexistenteDaResourceNotFound() {
        when(repositorio.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.similares(404L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe una publicación con id: 404");
    }
}
