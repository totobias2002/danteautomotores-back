package com.danteautomotores.security;

import com.danteautomotores.controller.AdminPublicacionController;
import com.danteautomotores.controller.PublicacionController;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PROD-01: un token malo nunca produce 500. En un endpoint público la request sigue sin autenticar;
 * en uno protegido la API responde 401 JSON {"error"}; con rol insuficiente, 403 JSON {"error"}.
 */
@WebMvcTest(controllers = {PublicacionController.class, AdminPublicacionController.class})
class SeguridadErroresTest extends SeguridadWebMvcTestBase {

    @MockBean
    private PublicacionService publicacionService;

    @Test
    void endpointPublicoConTokenMalformadoSigueDandoOk() throws Exception {
        mvc.perform(get("/api/publicaciones").header("Authorization", "Bearer basura"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointPublicoConTokenVencidoSigueDandoOk() throws Exception {
        mvc.perform(get("/api/publicaciones").header("Authorization", bearerVencido("admin@dante.com")))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoSinTokenDa401Json() throws Exception {
        mvc.perform(post("/api/publicaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void endpointProtegidoConTokenMalformadoDa401Json() throws Exception {
        mvc.perform(post("/api/publicaciones").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void endpointProtegidoConBearerVacioDa401Json() throws Exception {
        mvc.perform(post("/api/publicaciones").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void endpointProtegidoConTokenVencidoDa401Json() throws Exception {
        mvc.perform(post("/api/publicaciones").header("Authorization", bearerVencido("admin@dante.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void endpointProtegidoConTokenDeUsuarioBorradoDa401Json() throws Exception {
        String bearer = bearerPara("borrado@x.com", "ADMIN");
        when(userDetailsService.loadUserByUsername("borrado@x.com"))
                .thenThrow(new UsernameNotFoundException("borrado@x.com"));

        mvc.perform(post("/api/publicaciones").header("Authorization", bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void compradorConTokenValidoEnEndpointDeAdminDa403Json() throws Exception {
        mvc.perform(post("/api/publicaciones").header("Authorization", bearerPara("comprador@x.com", "COMPRADOR")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void listadoAdminSinTokenDa401Json() throws Exception {
        mvc.perform(get("/api/admin/publicaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void listadoAdminConTokenMalformadoDa401Json() throws Exception {
        mvc.perform(get("/api/admin/publicaciones").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isString());
    }

    // En el slice @WebMvcTest no hay actuator: un 404 prueba que Security dejó pasar la request (no es 401 ni 403).
    @Test
    void healthSinTokenNoLoBloqueaSecurity() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthLivenessSinTokenNoLoBloqueaSecurity() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthConTokenMalformadoNoLoBloqueaSecurity() throws Exception {
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer basura"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actuatorEnvSinTokenSigueDando401Json() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void healthPorPostSigueProtegido() throws Exception {
        mvc.perform(post("/actuator/health"))
                .andExpect(status().isUnauthorized());
    }
}
