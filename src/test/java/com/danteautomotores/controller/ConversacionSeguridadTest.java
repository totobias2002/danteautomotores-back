package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    private static final String MENSAJE_VALIDO = "{\"texto\":\"¿Puedo verlo el sábado?\"}";

    private ConversacionDetalleResponse detalle() {
        return ConversacionDetalleResponse.builder()
                .conversacion(respuesta())
                .mensajes(List.of(MensajeResponse.builder().id(900L).autor(AutorMensaje.USUARIO).texto("Hola")
                        .creadoEn(Instant.parse("2026-10-07T15:30:00Z")).leido(false).build()))
                .build();
    }

    @Test
    void sinTokenElHiloYElEnvioDan401() throws Exception {
        mvc.perform(get("/api/conversaciones/50"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/api/conversaciones/50/mensajes").contentType(MediaType.APPLICATION_JSON).content(MENSAJE_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void elAdminRecibe403EnElHiloYEnElEnvio() throws Exception {
        mvc.perform(get("/api/conversaciones/50").header("Authorization", admin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unCompradorLeeElHiloConElMailDelToken() throws Exception {
        when(conversacionService.obtenerMia(50L, "comprador@x.com")).thenReturn(detalle());

        mvc.perform(get("/api/conversaciones/50").header("Authorization", comprador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversacion.id").value(50))
                .andExpect(jsonPath("$.mensajes[0].id").value(900))
                .andExpect(jsonPath("$.mensajes[0].autor").value("USUARIO"))
                .andExpect(jsonPath("$.mensajes[0].creadoEn").value("2026-10-07T15:30:00Z"))
                .andExpect(jsonPath("$.mensajes[0].leido").value(false));

        verify(conversacionService).obtenerMia(50L, "comprador@x.com");
    }

    @Test
    void unCompradorEnviaUnMensajeConElMailDelToken() throws Exception {
        when(conversacionService.enviarMensaje(eq(50L), any(), eq("comprador@x.com")))
                .thenReturn(MensajeResponse.builder().id(901L).autor(AutorMensaje.USUARIO)
                        .texto("¿Puedo verlo el sábado?").creadoEn(Instant.parse("2026-10-07T15:31:00Z")).build());

        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(901))
                .andExpect(jsonPath("$.autor").value("USUARIO"))
                .andExpect(jsonPath("$.texto").value("¿Puedo verlo el sábado?"));

        ArgumentCaptor<MensajeRequest> captor = ArgumentCaptor.forClass(MensajeRequest.class);
        verify(conversacionService).enviarMensaje(eq(50L), captor.capture(), eq("comprador@x.com"));
        assertThat(captor.getValue().getTexto()).isEqualTo("¿Puedo verlo el sábado?");
        // El pedido no tiene dónde guardar autor ni usuario: solo el texto.
        assertThat(MensajeRequest.class.getDeclaredFields())
                .extracting(campo -> campo.getName())
                .containsExactly("texto");
    }

    @Test
    void unaConversacionAjenaOInexistenteDa404EnElHiloYEnElEnvio() throws Exception {
        when(conversacionService.obtenerMia(eq(77L), anyString()))
                .thenThrow(new ResourceNotFoundException("No existe la conversación"));
        when(conversacionService.enviarMensaje(eq(77L), any(), anyString()))
                .thenThrow(new ResourceNotFoundException("No existe la conversación"));

        mvc.perform(get("/api/conversaciones/77").header("Authorization", comprador()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la conversación"));
        mvc.perform(post("/api/conversaciones/77/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la conversación"));
    }

    @Test
    void unTextoVacioOEnBlancoDa400ConElCampo() throws Exception {
        for (String cuerpo : List.of("{\"texto\":\"\"}", "{\"texto\":\"    \"}", "{}")) {
            mvc.perform(post("/api/conversaciones/50/mensajes")
                            .header("Authorization", comprador())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.texto").exists());
        }

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unTextoDe2001CaracteresDa400ConElCampoYUnoDe2000SeAcepta() throws Exception {
        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.texto").exists());
        verifyNoInteractions(conversacionService);

        when(conversacionService.enviarMensaje(eq(50L), any(), anyString()))
                .thenReturn(MensajeResponse.builder().id(902L).autor(AutorMensaje.USUARIO).build());
        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"" + "a".repeat(2000) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void elLimiteDeEnvioDa429ConRetryAfter() throws Exception {
        when(conversacionService.enviarMensaje(eq(50L), any(), anyString()))
                .thenThrow(new LimiteDeIntentosException(
                        "Enviaste muchos mensajes seguidos. Esperá unos minutos y volvé a intentar.", 600));

        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unaConversacionCerradaOUnaCuentaIncompletaDanSuErrorEnElEnvio() throws Exception {
        when(conversacionService.enviarMensaje(eq(50L), any(), anyString()))
                .thenThrow(new ReglaDeNegocioException("Esta conversación está cerrada."))
                .thenThrow(new CuentaNoVerificadaException(List.of(DatoFaltante.DNI)));

        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Esta conversación está cerrada."));
        mvc.perform(post("/api/conversaciones/50/mensajes")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MENSAJE_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_VERIFICADA"))
                .andExpect(jsonPath("$.faltantes[0]").value("DNI"));
    }
}
