package com.danteautomotores.controller;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.dto.consulta.ConsultaResponse;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.service.ConsultaService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultaController.class)
class ConsultaSeguridadTest extends SeguridadWebMvcTestBase {

    private static final String BODY_VALIDO = "{\"publicacionId\":7,\"mensaje\":\"¿Sigue disponible?\"}";

    @MockBean
    private ConsultaService consultaService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private String admin() {
        return bearerPara("admin@x.com", "ADMIN");
    }

    private ConsultaResponse respuesta() {
        return ConsultaResponse.builder().id(1L).publicacionId(7L).nombreComprador("Ana Pérez")
                .emailComprador("comprador@x.com").telefonoComprador("+5491112345678")
                .mensaje("¿Sigue disponible?").build();
    }

    @Test
    void sinTokenResponde401ConElJsonDeError() throws Exception {
        mvc.perform(post("/api/consultas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(consultaService);
    }

    @Test
    void elAdminNoPuedeConsultar403() throws Exception {
        mvc.perform(post("/api/consultas")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(consultaService);
    }

    @Test
    void unCompradorConsultaYElServiceRecibeElMailDelToken() throws Exception {
        when(consultaService.crear(any(), eq("comprador@x.com"))).thenReturn(respuesta());

        mvc.perform(post("/api/consultas")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComprador").value("Ana Pérez"));

        verify(consultaService).crear(any(ConsultaRequest.class), eq("comprador@x.com"));
    }

    @Test
    void unCompradorConCuentaIncompletaRecibe403ConElCodigoYLosFaltantes() throws Exception {
        when(consultaService.crear(any(), anyString()))
                .thenThrow(new CuentaNoVerificadaException(List.of(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR)));

        mvc.perform(post("/api/consultas")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_VERIFICADA"))
                .andExpect(jsonPath("$.faltantes[0]").value("DNI"))
                .andExpect(jsonPath("$.faltantes[1]").value("EMAIL_SIN_CONFIRMAR"));
    }

    @Test
    void unCuerpoViejoConNombreMailYTelefonoNoRompeYEsosValoresSeIgnoran() throws Exception {
        when(consultaService.crear(any(), anyString())).thenReturn(respuesta());

        mvc.perform(post("/api/consultas")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"Hola\","
                                + "\"nombreComprador\":\"Otra Persona\",\"emailComprador\":\"otra@x.com\","
                                + "\"telefonoComprador\":\"123\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ConsultaRequest> captor = ArgumentCaptor.forClass(ConsultaRequest.class);
        verify(consultaService).crear(captor.capture(), eq("comprador@x.com"));
        assertThat(captor.getValue().getPublicacionId()).isEqualTo(7L);
        assertThat(captor.getValue().getMensaje()).isEqualTo("Hola");
        // El pedido no tiene dónde guardar los datos de contacto: solo publicación y mensaje.
        assertThat(ConsultaRequest.class.getDeclaredFields())
                .extracting(campo -> campo.getName())
                .containsExactlyInAnyOrder("publicacionId", "mensaje");
    }

    @Test
    void unMensajeEnBlancoDa400ConCampos() throws Exception {
        mvc.perform(post("/api/consultas")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.campos.mensaje").exists());

        verifyNoInteractions(consultaService);
    }

    @Test
    void unMensajeDeMasDe2000CaracteresDa400() throws Exception {
        mvc.perform(post("/api/consultas")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.mensaje").exists());

        verifyNoInteractions(consultaService);
    }

    @Test
    void elListadoPorPublicacionSigueSiendoSoloDelAdmin() throws Exception {
        when(consultaService.listarPorPublicacion(7L)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/consultas/publicacion/7").header("Authorization", comprador()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/consultas/publicacion/7").header("Authorization", admin()))
                .andExpect(status().isOk());
    }
}
