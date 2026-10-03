package com.danteautomotores.exception;

import com.danteautomotores.controller.AuthController;
import com.danteautomotores.controller.PublicacionController;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.service.AuthService;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato del formato de errores de la API: todo error responde {"error": "mensaje en español"} y, en las
 * validaciones, además {"campos": {campo: mensaje}}. Nunca se filtran stacktraces, clases ni mensajes internos.
 */
@WebMvcTest(controllers = {PublicacionController.class, AuthController.class})
class GlobalExceptionHandlerTest extends SeguridadWebMvcTestBase {

    private static final String MENSAJE_500 = "Ocurrió un error inesperado. Intentá de nuevo más tarde.";

    @MockBean
    private PublicacionService publicacionService;

    @MockBean
    private AuthService authService;

    private String admin() {
        return bearerPara("admin@dante.com", "ADMIN");
    }

    @Test
    void validacionDevuelveErrorYMapaDeCampos() throws Exception {
        mvc.perform(post("/api/publicaciones")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Datos inválidos"))
                .andExpect(jsonPath("$.campos.marca").isString());
    }

    @Test
    void recursoInexistenteDevuelve404ConElMensajeDelService() throws Exception {
        when(publicacionService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("No existe una publicación con id: 99"));

        mvc.perform(get("/api/publicaciones/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("No existe una publicación con id: 99"));
    }

    @Test
    void idNoNumericoDevuelve400SinFiltrarLaExcepcion() throws Exception {
        mvc.perform(get("/api/publicaciones/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(content().string(not(containsString("For input string"))))
                .andExpect(content().string(not(containsString("NumberFormatException"))));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/publicaciones")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void enumInvalidoDevuelve400() throws Exception {
        mvc.perform(patch("/api/publicaciones/1/estado")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INEXISTENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void rutaInexistenteDevuelve404ConError() throws Exception {
        mvc.perform(get("/api/ruta-que-no-existe")
                        .header("Authorization", admin()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void metodoNoPermitidoDevuelve405ConError() throws Exception {
        mvc.perform(delete("/api/publicaciones")
                        .header("Authorization", admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void errorNoControladoDevuelve500GenericoSinDetalleInterno() throws Exception {
        when(catalogoService.buscar(any()))
                .thenThrow(new RuntimeException("detalle interno secreto"));

        mvc.perform(get("/api/publicaciones"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(MENSAJE_500))
                .andExpect(content().string(not(containsString("detalle interno secreto"))))
                .andExpect(content().string(not(containsString("RuntimeException"))));
    }

    @Test
    void archivoDemasiadoGrandeDevuelve413() throws Exception {
        when(publicacionService.agregarFoto(anyLong(), any()))
                .thenThrow(new MaxUploadSizeExceededException(10485760L));

        mvc.perform(multipart("/api/publicaciones/1/fotos")
                        .file(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", admin()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(containsString("10 MB")));
    }

    @Test
    void faltaLaParteArchivoDevuelve400() throws Exception {
        mvc.perform(multipart("/api/publicaciones/1/fotos")
                        .header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void violacionDeIntegridadDevuelve409SinFiltrarElDetalle() throws Exception {
        doThrow(new DataIntegrityViolationException("violates fk_consulta"))
                .when(publicacionService).eliminar(1L);

        mvc.perform(delete("/api/publicaciones/1")
                        .header("Authorization", admin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(content().string(not(containsString("fk_consulta"))));
    }

    @Test
    void lockDeLaPublicacionNoObtenidoADevuelve409ConMensajeEnEspanolSinFiltrarElDetalle() throws Exception {
        when(publicacionService.agregarFoto(anyLong(), any()))
                .thenThrow(new CannotAcquireLockException("ERROR: canceling statement due to lock timeout on publicaciones"));

        mvc.perform(multipart("/api/publicaciones/1/fotos")
                        .file(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", admin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Otra operación está modificando este auto. Intentá de nuevo en unos segundos."))
                .andExpect(content().string(not(containsString("lock timeout"))));
    }

    @Test
    void fallaDeServicioExternoDevuelve502ConElMensajeDeLaExcepcion() throws Exception {
        when(publicacionService.agregarFoto(anyLong(), any()))
                .thenThrow(new ServicioExternoException("No se pudo subir la imagen. Intentá de nuevo en unos minutos."));

        mvc.perform(multipart("/api/publicaciones/1/fotos")
                        .file(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", admin()))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("No se pudo subir la imagen. Intentá de nuevo en unos minutos."));
    }

    @Test
    void accessDeniedLanzadoDesdeUnServiceDevuelve403yNoUn500() throws Exception {
        when(publicacionService.obtenerPorId(1L)).thenThrow(new AccessDeniedException("x"));

        mvc.perform(get("/api/publicaciones/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("No tenés permiso para realizar esta acción."));
    }

    @Test
    void reglaDeNegocioDevuelve400ConElMensajeDelService() throws Exception {
        doThrow(new ReglaDeNegocioException("La foto no pertenece a esta publicación"))
                .when(publicacionService).eliminarFoto(1L, 2L);

        mvc.perform(delete("/api/publicaciones/1/fotos/2")
                        .header("Authorization", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("La foto no pertenece a esta publicación"));
    }

    @Test
    void illegalArgumentExceptionAjenoALasReglasDeNegocioDevuelve500GenericoSinSuMensaje() throws Exception {
        doThrow(new IllegalArgumentException("Property 'foo' of class com.x.Y is invalid"))
                .when(publicacionService).eliminarFoto(1L, 2L);

        mvc.perform(delete("/api/publicaciones/1/fotos/2")
                        .header("Authorization", admin()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(MENSAJE_500))
                .andExpect(content().string(not(containsString("Property"))));
    }

    @Test
    void credencialesInvalidasEnElLoginDevuelven401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }
}
