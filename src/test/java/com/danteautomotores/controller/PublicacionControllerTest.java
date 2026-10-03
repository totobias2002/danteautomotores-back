package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.CambiarDestacadoRequest;
import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.ReordenarFotosRequest;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void adminReordenaLasFotosYLaRespuestaTraeLasFotosEnEseOrden() throws Exception {
        when(publicacionService.reordenarFotos(eq(1L), any(ReordenarFotosRequest.class)))
                .thenReturn(PublicacionResponse.builder().id(1L).build());

        mvc.perform(put("/api/publicaciones/1/fotos/orden")
                        .header("Authorization", bearerPara(ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fotoIds\":[3,1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<ReordenarFotosRequest> enviado = ArgumentCaptor.forClass(ReordenarFotosRequest.class);
        verify(publicacionService).reordenarFotos(eq(1L), enviado.capture());
        assertThat(enviado.getValue().getFotoIds()).containsExactly(3L, 1L, 2L);
    }

    @Test
    void reordenarConListaVaciaDa400ConElCampoEnElDetalle() throws Exception {
        mvc.perform(put("/api/publicaciones/1/fotos/orden")
                        .header("Authorization", bearerPara(ADMIN, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fotoIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fotoIds").exists());
    }

    @Test
    void compradorNoPuedeReordenarFotos() throws Exception {
        mvc.perform(put("/api/publicaciones/1/fotos/orden")
                        .header("Authorization", bearerPara(COMPRADOR, "COMPRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fotoIds\":[1]}"))
                .andExpect(status().isForbidden());
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
