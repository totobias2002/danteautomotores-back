package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.UsuarioDeConversacionResponse;
import com.danteautomotores.dto.usuario.UsuarioFichaResponse;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.service.UsuarioAdminService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUsuarioController.class)
class AdminUsuarioSeguridadTest extends SeguridadWebMvcTestBase {

    private static final String URL = "/api/admin/usuarios/7";

    @MockBean
    private UsuarioAdminService usuarioAdminService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private String admin() {
        return bearerPara("admin@x.com", "ADMIN");
    }

    private UsuarioFichaResponse ficha() {
        ConversacionResumenResponse fila = ConversacionResumenResponse.builder()
                .id(50L).tipo(TipoConversacion.COMPRA).estado(EstadoConversacion.ABIERTA)
                .ultimoMensajeEn(Instant.parse("2026-10-07T15:30:00Z")).ultimoMensaje("Hola").noLeidos(1)
                .usuario(UsuarioDeConversacionResponse.builder().id(7L).nombre("Ana").apellido("Lopez").email("ana@x.com").build())
                .build();
        return UsuarioFichaResponse.builder()
                .id(7L).nombre("Ana").apellido("Lopez").email("ana@x.com").telefono("+5491155550000").dni("30123456")
                .emailConfirmado(true).cuentaVerificada(false).faltantes(List.of(DatoFaltante.TELEFONO))
                .fechaRegistro(LocalDate.of(2026, 3, 15)).conversaciones(List.of(fila)).build();
    }

    @Test
    void sinTokenDa401ConElJsonDeError() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    void unCompradorRecibe403ConElJsonDeError() throws Exception {
        mvc.perform(get(URL).header("Authorization", comprador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    void elAdminRecibe200ConLasClavesDeLaFicha() throws Exception {
        when(usuarioAdminService.obtenerFicha(7L)).thenReturn(ficha());

        mvc.perform(get(URL).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Lopez"))
                .andExpect(jsonPath("$.email").value("ana@x.com"))
                .andExpect(jsonPath("$.telefono").value("+5491155550000"))
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(jsonPath("$.emailConfirmado").value(true))
                .andExpect(jsonPath("$.cuentaVerificada").value(false))
                .andExpect(jsonPath("$.faltantes[0]").value("TELEFONO"))
                .andExpect(jsonPath("$.fechaRegistro").value("2026-03-15"))
                .andExpect(jsonPath("$.conversaciones[0].id").value(50))
                .andExpect(jsonPath("$.conversaciones[0].noLeidos").value(1));
    }

    @Test
    void unIdInexistenteODeUnaCuentaAdminDa404ConElJsonDeError() throws Exception {
        when(usuarioAdminService.obtenerFicha(anyLong())).thenThrow(new ResourceNotFoundException("No existe el usuario"));

        mvc.perform(get("/api/admin/usuarios/999").header("Authorization", admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe el usuario"));
    }

    @Test
    void unIdNoNumericoDa400ConElJsonDeErrorYNoLlegaAlService() throws Exception {
        mvc.perform(get("/api/admin/usuarios/abc").header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(usuarioAdminService);
    }

    @Test
    void elToStringDeLaFichaNoImprimeDniTelefonoNiMail() {
        String texto = ficha().toString();

        assertThat(texto).doesNotContain("30123456").doesNotContain("+5491155550000").doesNotContain("ana@x.com");
        assertThat(texto).contains("Ana");
    }
}
