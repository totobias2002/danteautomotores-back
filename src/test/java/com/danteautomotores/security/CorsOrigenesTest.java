package com.danteautomotores.security;

import com.danteautomotores.controller.PublicacionController;
import com.danteautomotores.service.PublicacionService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** APP_CORS_ALLOWED_ORIGINS admite espacios después de las comas. */
@WebMvcTest(PublicacionController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=https://a.vercel.app,  https://b.com , ,https://c.com")
class CorsOrigenesTest extends SeguridadWebMvcTestBase {

    @MockBean
    private PublicacionService publicacionService;

    private void assertOrigenPermitido(String origen) throws Exception {
        mvc.perform(options("/api/publicaciones")
                        .header("Origin", origen)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    @Test
    void elPrimerOrigenSeAcepta() throws Exception {
        assertOrigenPermitido("https://a.vercel.app");
    }

    @Test
    void unOrigenDespuesDeUnaComaConEspaciosSeAcepta() throws Exception {
        assertOrigenPermitido("https://b.com");
        assertOrigenPermitido("https://c.com");
    }

    @Test
    void unOrigenNoListadoSeRechaza() throws Exception {
        mvc.perform(options("/api/publicaciones")
                        .header("Origin", "https://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
