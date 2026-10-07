package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.UsuarioDeConversacionResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.service.ConversacionAdminService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
