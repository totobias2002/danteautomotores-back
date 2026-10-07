package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.service.ConversacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.Instant;
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

@WebMvcTest(ConversacionController.class)
class ConversacionSeguridadTest extends SeguridadWebMvcTestBase {

    private static final String BODY_VALIDO = "{\"publicacionId\":7,\"mensaje\":\"¿Sigue disponible?\"}";

    @MockBean
    private ConversacionService conversacionService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private String admin() {
        return bearerPara("admin@x.com", "ADMIN");
    }

    private ConversacionResumenResponse respuesta() {
        return ConversacionResumenResponse.builder().id(50L).tipo(TipoConversacion.COMPRA)
                .estado(EstadoConversacion.ABIERTA).creadaEn(Instant.parse("2026-10-07T15:30:00Z"))
                .ultimoMensajeEn(Instant.parse("2026-10-07T15:30:00Z")).ultimoMensaje("¿Sigue disponible?")
                .ultimoMensajeAutor(AutorMensaje.USUARIO).build();
    }

    @Test
    void sinTokenPostYGetDan401ConElJsonDeError() throws Exception {
        mvc.perform(post("/api/conversaciones").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/conversaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void elAdminRecibe403EnPostYGet() throws Exception {
        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/conversaciones").header("Authorization", admin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unCompradorInicia200YElServiceRecibeElMailDelToken() throws Exception {
        when(conversacionService.iniciarCompra(any(), eq("comprador@x.com"))).thenReturn(respuesta());

        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.creadaEn").value("2026-10-07T15:30:00Z"));

        verify(conversacionService).iniciarCompra(any(ConversacionRequest.class), eq("comprador@x.com"));
    }

    @Test
    void unCompradorListaSusConversacionesConElMailDelToken() throws Exception {
        when(conversacionService.listarMias("comprador@x.com")).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/conversaciones").header("Authorization", comprador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50));

        verify(conversacionService).listarMias("comprador@x.com");
    }

    @Test
    void unaCuentaIncompletaRecibe403ConElCodigoYLosFaltantes() throws Exception {
        when(conversacionService.iniciarCompra(any(), anyString()))
                .thenThrow(new CuentaNoVerificadaException(List.of(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR)));

        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_VERIFICADA"))
                .andExpect(jsonPath("$.faltantes[0]").value("DNI"))
                .andExpect(jsonPath("$.faltantes[1]").value("EMAIL_SIN_CONFIRMAR"));
    }

    @Test
    void unCuerpoConTipoEstadoYUsuarioDeMasSeIgnora() throws Exception {
        when(conversacionService.iniciarCompra(any(), anyString())).thenReturn(respuesta());

        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"Hola\","
                                + "\"tipo\":\"COTIZACION\",\"estado\":\"CERRADA\",\"usuarioId\":1}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ConversacionRequest> captor = ArgumentCaptor.forClass(ConversacionRequest.class);
        verify(conversacionService).iniciarCompra(captor.capture(), eq("comprador@x.com"));
        assertThat(captor.getValue().getPublicacionId()).isEqualTo(7L);
        assertThat(captor.getValue().getMensaje()).isEqualTo("Hola");
        // El pedido no tiene dónde guardar tipo, estado ni usuario: solo publicación y mensaje (T-04-08).
        assertThat(ConversacionRequest.class.getDeclaredFields())
                .extracting(campo -> campo.getName())
                .containsExactlyInAnyOrder("publicacionId", "mensaje");
    }

    @Test
    void sinPublicacionIdDa400ConElCampo() throws Exception {
        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.campos.publicacionId").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unMensajeDeMasDe2000CaracteresDa400ConElCampo() throws Exception {
        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.mensaje").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unMensajeDeExactamente2000CaracteresSeAcepta() throws Exception {
        when(conversacionService.iniciarCompra(any(), anyString())).thenReturn(respuesta());

        mvc.perform(post("/api/conversaciones")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicacionId\":7,\"mensaje\":\"" + "a".repeat(2000) + "\"}"))
                .andExpect(status().isOk());
    }
}
