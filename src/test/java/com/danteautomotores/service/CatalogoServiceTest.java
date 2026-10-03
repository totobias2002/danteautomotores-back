package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.OrdenCatalogo;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.repository.PublicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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
}
