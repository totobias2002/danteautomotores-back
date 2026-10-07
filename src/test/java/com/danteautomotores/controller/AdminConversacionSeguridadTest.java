package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.dto.conversacion.UsuarioDeConversacionResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.service.ConversacionAdminService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminConversacionController.class)
class AdminConversacionSeguridadTest extends SeguridadWebMvcTestBase {

    private static final String URL = "/api/admin/conversaciones";

    @MockBean
    private ConversacionAdminService conversacionAdminService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private String admin() {
        return bearerPara("admin@x.com", "ADMIN");
    }

    private PaginaResponse<ConversacionResumenResponse> pagina() {
        ConversacionResumenResponse fila = ConversacionResumenResponse.builder()
                .id(50L).tipo(TipoConversacion.COMPRA).estado(EstadoConversacion.ABIERTA)
                .creadaEn(Instant.parse("2026-10-07T15:30:00Z")).ultimoMensajeEn(Instant.parse("2026-10-07T15:30:00Z"))
                .ultimoMensaje("Hola").ultimoMensajeAutor(AutorMensaje.USUARIO).noLeidos(2)
                .usuario(UsuarioDeConversacionResponse.builder().id(7L).nombre("Ana").apellido("Lopez").email("ana@x.com").build())
                .build();
        return new PaginaResponse<>(List.of(fila), 1, 20, 1, 1);
    }

    @Test
    void sinTokenDa401ConElJsonDeError() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void unCompradorRecibe403ConElJsonDeError() throws Exception {
        mvc.perform(get(URL).header("Authorization", comprador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get(URL).param("soloNoLeidas", "true").header("Authorization", comprador()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void elAdminRecibe200ConLaEstructuraDePagina() throws Exception {
        when(conversacionAdminService.listar(any(), any(), anyBoolean(), anyInt())).thenReturn(pagina());

        mvc.perform(get(URL).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(50))
                .andExpect(jsonPath("$.contenido[0].noLeidos").value(2))
                .andExpect(jsonPath("$.contenido[0].usuario.nombre").value("Ana"))
                .andExpect(jsonPath("$.contenido[0].usuario.apellido").value("Lopez"))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(20))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void sinParametrosPideTodasLasConversacionesDeLaPrimeraPagina() throws Exception {
        when(conversacionAdminService.listar(any(), any(), anyBoolean(), anyInt())).thenReturn(pagina());

        mvc.perform(get(URL).header("Authorization", admin())).andExpect(status().isOk());

        verify(conversacionAdminService).listar(null, null, false, 1);
    }

    @Test
    void losFiltrosDeLaUrlLlegananAlService() throws Exception {
        when(conversacionAdminService.listar(any(), any(), anyBoolean(), anyInt())).thenReturn(pagina());

        mvc.perform(get(URL)
                        .param("tipo", "COTIZACION").param("estado", "CERRADA")
                        .param("soloNoLeidas", "true").param("pagina", "3")
                        .header("Authorization", admin()))
                .andExpect(status().isOk());

        verify(conversacionAdminService).listar(TipoConversacion.COTIZACION, EstadoConversacion.CERRADA, true, 3);
    }

    @Test
    void unTipoInvalidoDa400ConElJsonDeError() throws Exception {
        mvc.perform(get(URL).param("tipo", "NAVE").header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get(URL).param("estado", "TODAS").header("Authorization", admin()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void elJsonDeUnaFilaNoTraeDniNiTelefono() throws Exception {
        when(conversacionAdminService.listar(any(), any(), anyBoolean(), anyInt())).thenReturn(pagina());

        MvcResult resultado = mvc.perform(get(URL).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andReturn();

        String json = resultado.getResponse().getContentAsString().toLowerCase();
        assertThat(json).doesNotContain("dni").doesNotContain("telefono");
    }

    @Test
    void laRespuestaDelCompradorNoTraeLaClaveUsuario() throws Exception {
        // El campo usuario es NON_NULL: una fila sin usuario (la que arma el comprador) no lo lleva en el JSON.
        ConversacionResumenResponse sinUsuario = ConversacionResumenResponse.builder().id(1L).build();
        String json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                .writeValueAsString(sinUsuario);

        assertThat(json).doesNotContain("usuario");
    }

    // ---- El hilo de la agencia (04-06): 401 sin token, 403 al comprador, 200 al admin ----

    private static final String HILO = URL + "/50";
    private static final String CUERPO_VALIDO = "{\"texto\":\"Hola, pasen cuando quieran\"}";

    private ConversacionResumenResponse resumen() {
        return ConversacionResumenResponse.builder().id(50L).tipo(TipoConversacion.COMPRA)
                .estado(EstadoConversacion.CERRADA).noLeidos(0).build();
    }

    private void stubsDelAdmin() {
        when(conversacionAdminService.obtener(50L)).thenReturn(ConversacionDetalleResponse.builder()
                .conversacion(resumen()).mensajes(List.of()).build());
        when(conversacionAdminService.responder(eq(50L), any(MensajeRequest.class), eq("admin@x.com")))
                .thenReturn(MensajeResponse.builder().id(9L).autor(AutorMensaje.AGENCIA).texto("Hola").build());
        when(conversacionAdminService.marcarLeida(50L)).thenReturn(NoLeidosResponse.builder().noLeidos(0).conversaciones(0).build());
        when(conversacionAdminService.cerrar(50L)).thenReturn(resumen());
        when(conversacionAdminService.reabrir(50L)).thenReturn(resumen());
    }

    @Test
    void cadaEndpointDelHiloDa401SinToken() throws Exception {
        mvc.perform(get(HILO)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists());
        mvc.perform(post(HILO + "/mensajes").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(HILO + "/leida")).andExpect(status().isUnauthorized());
        mvc.perform(post(HILO + "/cerrar")).andExpect(status().isUnauthorized());
        mvc.perform(post(HILO + "/reabrir")).andExpect(status().isUnauthorized());

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void cadaEndpointDelHiloDa403AlComprador() throws Exception {
        String token = comprador();
        mvc.perform(get(HILO).header("Authorization", token)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post(HILO + "/mensajes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
        mvc.perform(post(HILO + "/leida").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post(HILO + "/cerrar").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post(HILO + "/reabrir").header("Authorization", token)).andExpect(status().isForbidden());

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void elAdminRecibe200EnCadaEndpointDelHilo() throws Exception {
        stubsDelAdmin();
        String token = admin();

        mvc.perform(get(HILO).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversacion.id").value(50))
                .andExpect(jsonPath("$.mensajes").isArray());
        mvc.perform(post(HILO + "/mensajes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autor").value("AGENCIA"));
        mvc.perform(post(HILO + "/leida").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidos").value(0));
        mvc.perform(post(HILO + "/cerrar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
        mvc.perform(post(HILO + "/reabrir").header("Authorization", token)).andExpect(status().isOk());

        // El autor es la cuenta del token: el cuerpo no puede elegirlo.
        verify(conversacionAdminService).responder(eq(50L), any(MensajeRequest.class), eq("admin@x.com"));
    }

    @Test
    void unTextoVacioEnBlancoOSinTextoDa400ConCamposTextoYNoLlegaAlService() throws Exception {
        String token = admin();
        for (String cuerpo : List.of("{\"texto\":\"\"}", "{\"texto\":\"    \"}", "{}")) {
            mvc.perform(post(HILO + "/mensajes").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.texto").exists());
        }

        verifyNoInteractions(conversacionAdminService);
    }

    @Test
    void unTextoDe2001CaracteresDa400ConCamposTextoYUnoDe2000Pasa() throws Exception {
        stubsDelAdmin();
        String token = admin();

        mvc.perform(post(HILO + "/mensajes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.texto").exists());
        mvc.perform(post(HILO + "/mensajes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"" + "a".repeat(2000) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unaConversacionInexistenteDa404ConElJsonDeError() throws Exception {
        when(conversacionAdminService.obtener(anyLong())).thenThrow(new ResourceNotFoundException("No existe la conversación"));
        when(conversacionAdminService.cerrar(anyLong())).thenThrow(new ResourceNotFoundException("No existe la conversación"));

        mvc.perform(get(URL + "/999").header("Authorization", admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la conversación"));
        mvc.perform(post(URL + "/999/cerrar").header("Authorization", admin()))
                .andExpect(status().isNotFound());
    }
}
