package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.FacetasResponse;
import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El catálogo público (sin token): forma de la página, binding de los filtros y parámetros que el cliente no puede mandar. */
@WebMvcTest(PublicacionController.class)
class PublicacionControllerCatalogoTest extends SeguridadWebMvcTestBase {

    @MockBean
    private PublicacionService publicacionService;

    private FiltrosCatalogo filtrosRecibidos() {
        ArgumentCaptor<FiltrosCatalogo> captor = ArgumentCaptor.forClass(FiltrosCatalogo.class);
        verify(catalogoService).buscar(captor.capture());
        return captor.getValue();
    }

    @Test
    void listadoSinTokenDevuelveLaFormaDePagina() throws Exception {
        PublicacionResumenResponse auto = PublicacionResumenResponse.builder().id(5L).marca("Toyota").build();
        when(catalogoService.buscar(any())).thenReturn(new PaginaResponse<>(List.of(auto), 2, 24, 30, 2));

        mvc.perform(get("/api/publicaciones?pagina=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(5))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.tamanio").value(24))
                .andExpect(jsonPath("$.totalElementos").value(30))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void losFiltrosDeListaRepetidosLlegananAlServicio() throws Exception {
        when(catalogoService.buscar(any())).thenReturn(new PaginaResponse<>(List.of(), 2, 24, 0, 0));

        mvc.perform(get("/api/publicaciones?pagina=2&marca=A&marca=B&tipo=SEDAN&ofertas=true&precioMax=1000"))
                .andExpect(status().isOk());

        FiltrosCatalogo f = filtrosRecibidos();
        assertThat(f.getPagina()).isEqualTo(2);
        assertThat(f.getMarca()).containsExactly("A", "B");
        assertThat(f.getTipo()).containsExactly(TipoCarroceria.SEDAN);
        assertThat(f.getOfertas()).isTrue();
        assertThat(f.getPrecioMax()).isEqualByComparingTo("1000");
    }

    @Test
    void unTipoInvalidoDa400ConError() throws Exception {
        mvc.perform(get("/api/publicaciones?tipo=NAVE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void unaPaginaIlegibleCaeALaPrimeraEnVezDeRomper() throws Exception {
        when(catalogoService.buscar(any())).thenReturn(new PaginaResponse<>(List.of(), 1, 24, 0, 0));

        mvc.perform(get("/api/publicaciones?pagina=abc")).andExpect(status().isOk());

        assertThat(filtrosRecibidos().getPagina()).isNull(); // el servicio la lleva a 1
    }

    @Test
    void elClienteNoPuedeElegirTamanioNiOrdenarPorUnCampoInterno() throws Exception {
        when(catalogoService.buscar(any())).thenReturn(new PaginaResponse<>(List.of(), 1, 24, 0, 0));

        mvc.perform(get("/api/publicaciones?size=1000&sort=admin.email&page=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(24));

        FiltrosCatalogo f = filtrosRecibidos();
        assertThat(f.getPagina()).isNull();
        assertThat(f.getOrden()).isNull();
        assertThat(f.getMarca()).isNull();
    }

    @Test
    void losDemasEndpointsSiguenYendoASuMetodo() throws Exception {
        when(catalogoService.destacados(any())).thenReturn(List.of());
        when(publicacionService.obtenerPorId(7L)).thenReturn(PublicacionResponse.builder().id(7L).build());

        mvc.perform(get("/api/publicaciones/destacados")).andExpect(status().isOk());
        mvc.perform(get("/api/publicaciones/7")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));

        verify(catalogoService).destacados(any());
        verify(publicacionService).obtenerPorId(7L);
    }

    @Test
    void facetasSinTokenLlegaAlServicioSinAgencia() throws Exception {
        when(catalogoService.facetas(any())).thenReturn(FacetasResponse.builder()
                .marcas(List.of(FacetasResponse.Conteo.builder().valor("Toyota").cantidad(2).build())).build());

        mvc.perform(get("/api/publicaciones/facetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcas[0].valor").value("Toyota"))
                .andExpect(jsonPath("$.marcas[0].cantidad").value(2));

        verify(catalogoService).facetas(isNull());
    }

    @Test
    void facetasConAgenciaIdSeLaPasaAlServicio() throws Exception {
        when(catalogoService.facetas(any())).thenReturn(FacetasResponse.builder().build());

        mvc.perform(get("/api/publicaciones/facetas?agenciaId=3")).andExpect(status().isOk());

        verify(catalogoService).facetas(eq(3L));
    }

    @Test
    void similaresSinTokenLlegaAlServicioSinLimiteYNoVaAObtenerPorId() throws Exception {
        when(catalogoService.similares(eq(7L), any())).thenReturn(List.of(
                PublicacionResumenResponse.builder().id(9L).marca("Toyota").build()));

        mvc.perform(get("/api/publicaciones/7/similares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));

        verify(catalogoService).similares(7L, null);
        verify(publicacionService, never()).obtenerPorId(any());
    }

    @Test
    void similaresConLimiteSeLoPasaAlServicio() throws Exception {
        when(catalogoService.similares(eq(7L), any())).thenReturn(List.of());

        mvc.perform(get("/api/publicaciones/7/similares?limite=6")).andExpect(status().isOk());

        verify(catalogoService).similares(7L, 6);
    }

    @Test
    void similaresDeUnAutoInexistenteDa404ConError() throws Exception {
        when(catalogoService.similares(eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("No existe una publicación con id: 404"));

        mvc.perform(get("/api/publicaciones/404/similares"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe una publicación con id: 404"));
    }
}
