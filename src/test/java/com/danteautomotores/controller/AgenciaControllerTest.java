package com.danteautomotores.controller;

import com.danteautomotores.dto.agencia.AgenciaResponse;
import com.danteautomotores.service.AgenciaService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgenciaController.class)
class AgenciaControllerTest extends SeguridadWebMvcTestBase {

    private static final String BODY_VALIDO = "{\"nombre\":\"Dante Automotores\",\"emailContacto\":\"ventas@dante.com\"}";

    @MockBean
    private AgenciaService agenciaService;

    private String admin() {
        return bearerPara("admin@dante.com", "ADMIN");
    }

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private AgenciaResponse agencia() {
        return AgenciaResponse.builder().id(1L).nombre("Dante Automotores").slug("dante-automotores").build();
    }

    @Test
    void listarEsPublico() throws Exception {
        when(agenciaService.listar()).thenReturn(List.of(agencia()));

        mvc.perform(get("/api/agencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("dante-automotores"));
    }

    @Test
    void adminCreaUnaAgencia() throws Exception {
        when(agenciaService.crear(any())).thenReturn(agencia());

        mvc.perform(post("/api/agencias")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void adminEditaUnaAgencia() throws Exception {
        when(agenciaService.actualizar(eq(1L), any())).thenReturn(agencia());

        mvc.perform(put("/api/agencias/1")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void adminEliminaUnaAgenciaYRecibe204() throws Exception {
        mvc.perform(delete("/api/agencias/1")
                        .header("Authorization", admin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarUnaAgenciaConAutosDa400() throws Exception {
        doThrow(new IllegalArgumentException("No se puede eliminar la agencia porque tiene autos publicados."))
                .when(agenciaService).eliminar(1L);

        mvc.perform(delete("/api/agencias/1")
                        .header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compradorNoPuedeCrearUnaAgencia() throws Exception {
        mvc.perform(post("/api/agencias")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void compradorNoPuedeEditarUnaAgencia() throws Exception {
        mvc.perform(put("/api/agencias/1")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void compradorNoPuedeEliminarUnaAgencia() throws Exception {
        mvc.perform(delete("/api/agencias/1")
                        .header("Authorization", comprador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenNoSePuedeCrearEditarNiEliminar() throws Exception {
        mvc.perform(post("/api/agencias").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/agencias/1").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/agencias/1"))
                .andExpect(status().isUnauthorized());
    }
}
