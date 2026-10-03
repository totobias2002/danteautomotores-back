package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.CambiarDestacadoRequest;
import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicacionController.class)
class PublicacionControllerTest extends SeguridadWebMvcTestBase {

    @MockBean
    private PublicacionService publicacionService;

    private static final String ADMIN = "admin@dante.com";
    private static final String COMPRADOR = "comprador@x.com";

    @Test
    void adminMarcaUnAutoComoDestacadoYLaRespuestaTraeElFlag() throws Exception {
        when(publicacionService.cambiarDestacado(eq(1L), any(CambiarDestacadoRequest.class)))
                .thenReturn(PublicacionResponse.builder().id(1L).destacado(true).build());

        mvc.perform(patch("/api/publicaciones/1/destacado")
                        .header("Authorization", bearerPara(ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destacado\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destacado").value(true));
    }

    @Test
    void destacadoSinElCampoDa400ConElCampoEnElDetalle() throws Exception {
        mvc.perform(patch("/api/publicaciones/1/destacado")
                        .header("Authorization", bearerPara(ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos inválidos"))
                .andExpect(jsonPath("$.campos.destacado").exists());
    }

    @Test
    void compradorNoPuedeCambiarElDestacado() throws Exception {
        mvc.perform(patch("/api/publicaciones/1/destacado")
                        .header("Authorization", bearerPara(COMPRADOR, "COMPRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destacado\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenElDestacadoDa401() throws Exception {
        mvc.perform(patch("/api/publicaciones/1/destacado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destacado\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void adminCambiaElEstadoAReservado() throws Exception {
        when(publicacionService.cambiarEstado(eq(1L), any(CambiarEstadoRequest.class)))
                .thenReturn(PublicacionResponse.builder().id(1L).estado(EstadoPublicacion.RESERVADO).build());

        mvc.perform(patch("/api/publicaciones/1/estado")
                        .header("Authorization", bearerPara(ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESERVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADO"));
    }
}
