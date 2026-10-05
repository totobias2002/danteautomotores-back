package com.danteautomotores.controller;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.CambiarContrasenaRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.service.UsuarioService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest extends SeguridadWebMvcTestBase {

    private static final String BODY_VALIDO =
            "{\"nombre\":\"Ana\",\"apellido\":\"Pérez\",\"telefono\":\"011 15 1234-5678\",\"dni\":\"30.123.456\"}";

    @MockBean
    private UsuarioService usuarioService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private UsuarioResponse perfil() {
        return UsuarioResponse.builder()
                .id(7L).nombre("Ana").apellido("Pérez").email("comprador@x.com")
                .telefono("+5491112345678").dni("30123456").rol("COMPRADOR")
                .emailConfirmado(false).tieneContrasena(true).tieneGoogle(false)
                .cuentaVerificada(false).faltantes(List.of(DatoFaltante.EMAIL_SIN_CONFIRMAR))
                .build();
    }

    @Test
    void sinTokenGetYPutResponden401ConElJsonDeError() throws Exception {
        mvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(put("/api/usuarios/me").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void getDevuelveElPerfilDelDuenoYElServiceSeInvocaConElMailDelToken() throws Exception {
        when(usuarioService.obtenerPerfil("comprador@x.com")).thenReturn(perfil());

        mvc.perform(get("/api/usuarios/me").header("Authorization", comprador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("comprador@x.com"))
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(jsonPath("$.telefono").value("+5491112345678"))
                .andExpect(jsonPath("$.cuentaVerificada").value(false))
                .andExpect(jsonPath("$.faltantes[0]").value("EMAIL_SIN_CONFIRMAR"))
                .andExpect(jsonPath("$.tieneContrasena").value(true))
                .andExpect(jsonPath("$.tieneGoogle").value(false));

        verify(usuarioService).obtenerPerfil("comprador@x.com");
    }

    @Test
    void getIgnoraUnIdOUnMailPedidoPorParametro() throws Exception {
        when(usuarioService.obtenerPerfil("comprador@x.com")).thenReturn(perfil());

        mvc.perform(get("/api/usuarios/me?id=99&email=otro@x.com").header("Authorization", comprador()))
                .andExpect(status().isOk());

        verify(usuarioService).obtenerPerfil("comprador@x.com");
        verify(usuarioService, never()).obtenerPerfil("otro@x.com");
    }

    @Test
    void putConCuerpoVacioDa400ConCampos() throws Exception {
        mvc.perform(put("/api/usuarios/me")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.apellido").exists())
                .andExpect(jsonPath("$.campos.telefono").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void putValidoDa200YElServiceRecibeElMailDelToken() throws Exception {
        when(usuarioService.actualizarPerfil(eq("comprador@x.com"), any())).thenReturn(perfil());

        mvc.perform(put("/api/usuarios/me")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        ArgumentCaptor<ActualizarPerfilRequest> captor = ArgumentCaptor.forClass(ActualizarPerfilRequest.class);
        verify(usuarioService).actualizarPerfil(eq("comprador@x.com"), captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana");
        assertThat(captor.getValue().getApellido()).isEqualTo("Pérez");
        assertThat(captor.getValue().getTelefono()).isEqualTo("011 15 1234-5678");
        assertThat(captor.getValue().getDni()).isEqualTo("30.123.456");
    }

    @Test
    void putConEmailYRolDeMasNoRompeYNoLlegaAlServiceComoCampos() throws Exception {
        when(usuarioService.actualizarPerfil(anyString(), any())).thenReturn(perfil());

        mvc.perform(put("/api/usuarios/me")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Pérez\",\"telefono\":\"011 15 1234-5678\","
                                + "\"email\":\"otro@x.com\",\"rol\":\"ADMIN\",\"googleSub\":\"x\"}"))
                .andExpect(status().isOk());

        // El mail sigue saliendo del token y el request no tiene dónde guardar email ni rol.
        verify(usuarioService).actualizarPerfil(eq("comprador@x.com"), any());
        assertThat(Arrays.stream(ActualizarPerfilRequest.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("email", "rol", "googleSub");
    }

    // ---- POST /me/contrasena y /me/reenviar-confirmacion (03-10) ----

    private static final String CAMBIO_VALIDO = "{\"actual\":\"actual-1234\",\"nueva\":\"nueva-5678\"}";

    @Test
    void sinTokenLosDosPostDeCuentaResponden401() throws Exception {
        mvc.perform(post("/api/usuarios/me/contrasena").contentType(MediaType.APPLICATION_JSON).content(CAMBIO_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/api/usuarios/me/reenviar-confirmacion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasenaConTokenDa200ConLaSesionNuevaYElMailDelToken() throws Exception {
        when(usuarioService.cambiarContrasena(eq("comprador@x.com"), any()))
                .thenReturn(AuthResponse.builder().token("jwt-nuevo").email("comprador@x.com").rol("COMPRADOR").build());

        mvc.perform(post("/api/usuarios/me/contrasena")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAMBIO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-nuevo"));

        ArgumentCaptor<CambiarContrasenaRequest> captor = ArgumentCaptor.forClass(CambiarContrasenaRequest.class);
        verify(usuarioService).cambiarContrasena(eq("comprador@x.com"), captor.capture());
        assertThat(captor.getValue().getActual()).isEqualTo("actual-1234");
        assertThat(captor.getValue().getNueva()).isEqualTo("nueva-5678");
    }

    @Test
    void cambiarContrasenaConLaNuevaCortaDa400ConCampos() throws Exception {
        mvc.perform(post("/api/usuarios/me/contrasena")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"actual-1234\",\"nueva\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nueva").value("La contraseña debe tener entre 8 y 72 caracteres"));
        mvc.perform(post("/api/usuarios/me/contrasena")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.actual").exists())
                .andExpect(jsonPath("$.campos.nueva").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasenaConLaActualIncorrectaDa400ConElMensajeDelService() throws Exception {
        when(usuarioService.cambiarContrasena(anyString(), any()))
                .thenThrow(new ReglaDeNegocioException("La contraseña actual no es correcta."));

        mvc.perform(post("/api/usuarios/me/contrasena")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAMBIO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña actual no es correcta."));
    }

    @Test
    void cambiarContrasenaExcedidoElLimiteDa429ConRetryAfter() throws Exception {
        when(usuarioService.cambiarContrasena(anyString(), any()))
                .thenThrow(new LimiteDeIntentosException("Demasiados intentos.", 900));

        mvc.perform(post("/api/usuarios/me/contrasena")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAMBIO_VALIDO))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    @Test
    void reenviarConfirmacionConTokenDa200ConElMensajeYElMailDelToken() throws Exception {
        when(usuarioService.reenviarConfirmacion("comprador@x.com")).thenReturn("Tu mail ya está confirmado.");

        mvc.perform(post("/api/usuarios/me/reenviar-confirmacion").header("Authorization", comprador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu mail ya está confirmado."));

        verify(usuarioService).reenviarConfirmacion("comprador@x.com");
    }

    @Test
    void reenviarConfirmacionExcedidoElLimiteDa429ConRetryAfter() throws Exception {
        when(usuarioService.reenviarConfirmacion(anyString()))
                .thenThrow(new LimiteDeIntentosException("Ya te mandamos varios mails.", 3600));

        mvc.perform(post("/api/usuarios/me/reenviar-confirmacion").header("Authorization", comprador()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"));
    }

    @Test
    void unaReglaDeNegocioDelServiceDa400ConSuMensaje() throws Exception {
        when(usuarioService.actualizarPerfil(anyString(), any()))
                .thenThrow(new ReglaDeNegocioException("Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña."));

        mvc.perform(put("/api/usuarios/me")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña."));
    }
}
