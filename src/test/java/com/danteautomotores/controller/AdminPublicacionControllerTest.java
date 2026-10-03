package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.ImpactoEliminacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPublicacionController.class)
class AdminPublicacionControllerTest extends SeguridadWebMvcTestBase {

    @MockBean
    private PublicacionService publicacionService;

    private PublicacionResponse auto(long id, EstadoPublicacion estado) {
        return PublicacionResponse.builder().id(id).marca("Toyota").modelo("Corolla").estado(estado).build();
    }

    @Test
    void adminListaAutosDeTodosLosEstados() throws Exception {
        when(publicacionService.listarParaAdmin()).thenReturn(List.of(
                auto(1, EstadoPublicacion.DISPONIBLE),
                auto(2, EstadoPublicacion.RESERVADO),
                auto(3, EstadoPublicacion.VENDIDO)));

        mvc.perform(get("/api/admin/publicaciones")
                        .header("Authorization", bearerPara("admin@dante.com", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estado").value(hasItems("DISPONIBLE", "RESERVADO", "VENDIDO")));
    }

    @Test
    void sinTokenElPanelAdminDa401ConError() throws Exception {
        mvc.perform(get("/api/admin/publicaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void adminConsultaElImpactoDeEliminarUnAuto() throws Exception {
        when(publicacionService.obtenerImpactoEliminacion(5L))
                .thenReturn(ImpactoEliminacionResponse.builder().cantidadConsultas(3).cantidadFavoritos(2).build());

        mvc.perform(get("/api/admin/publicaciones/5/impacto-eliminacion")
                        .header("Authorization", bearerPara("admin@dante.com", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadConsultas").value(3))
                .andExpect(jsonPath("$.cantidadFavoritos").value(2));
    }

    @Test
    void compradorNoPuedeVerElImpactoDeEliminacion() throws Exception {
        mvc.perform(get("/api/admin/publicaciones/5/impacto-eliminacion")
                        .header("Authorization", bearerPara("comprador@x.com", "COMPRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenElImpactoDeEliminacionDa401() throws Exception {
        mvc.perform(get("/api/admin/publicaciones/5/impacto-eliminacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elImpactoDeUnAutoInexistenteDa404ConError() throws Exception {
        when(publicacionService.obtenerImpactoEliminacion(99L))
                .thenThrow(new ResourceNotFoundException("No existe una publicación con id: 99"));

        mvc.perform(get("/api/admin/publicaciones/99/impacto-eliminacion")
                        .header("Authorization", bearerPara("admin@dante.com", "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe una publicación con id: 99"));
    }

    @Test
    void compradorNoPuedeListarElPanelAdmin() throws Exception {
        mvc.perform(get("/api/admin/publicaciones")
                        .header("Authorization", bearerPara("comprador@x.com", "COMPRADOR")))
                .andExpect(status().isForbidden());
    }
}
