package com.danteautomotores.controller;

import com.danteautomotores.dto.solicitudventa.SolicitudVentaResponse;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoSolicitudVenta;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.service.SolicitudVentaService;
import com.danteautomotores.support.SeguridadWebMvcTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudVentaController.class)
class SolicitudVentaSeguridadTest extends SeguridadWebMvcTestBase {

    private static final String BODY_VALIDO = "{\"marca\":\"Ford\",\"modelo\":\"Fiesta\",\"anio\":2018,"
            + "\"nombreVendedor\":\"Luis Gómez\",\"telefonoVendedor\":\"11 5555-0000\"}";

    @MockBean
    private SolicitudVentaService solicitudVentaService;

    private String comprador() {
        return bearerPara("comprador@x.com", "COMPRADOR");
    }

    private String admin() {
        return bearerPara("admin@x.com", "ADMIN");
    }

    private SolicitudVentaResponse respuesta() {
        return SolicitudVentaResponse.builder().id(1L).marca("Ford").modelo("Fiesta").anio(2018)
                .estado(EstadoSolicitudVenta.PENDIENTE).build();
    }

    @Test
    void sinTokenElAltaResponde401ConElJsonDeError() throws Exception {
        mvc.perform(post("/api/solicitudes-venta").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(solicitudVentaService);
    }

    @Test
    void elAdminNoPuedeCrearUnaSolicitud403() throws Exception {
        mvc.perform(post("/api/solicitudes-venta")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(solicitudVentaService);
    }

    @Test
    void unCompradorCreaYElServiceRecibeElMailDelToken() throws Exception {
        when(solicitudVentaService.crear(any(), eq("comprador@x.com"))).thenReturn(respuesta());

        mvc.perform(post("/api/solicitudes-venta")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(solicitudVentaService).crear(any(), eq("comprador@x.com"));
    }

    @Test
    void unCompradorConCuentaIncompletaRecibe403ConElCodigoYLosFaltantes() throws Exception {
        when(solicitudVentaService.crear(any(), anyString()))
                .thenThrow(new CuentaNoVerificadaException(List.of(DatoFaltante.TELEFONO, DatoFaltante.DNI)));

        mvc.perform(post("/api/solicitudes-venta")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_NO_VERIFICADA"))
                .andExpect(jsonPath("$.faltantes[0]").value("TELEFONO"))
                .andExpect(jsonPath("$.faltantes[1]").value("DNI"));
    }

    @Test
    void elListadoEsSoloDelAdmin() throws Exception {
        when(solicitudVentaService.listarTodas()).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/solicitudes-venta").header("Authorization", comprador()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/solicitudes-venta").header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    @Test
    void elCambioDeEstadoEsSoloDelAdmin() throws Exception {
        when(solicitudVentaService.cambiarEstado(eq(1L), any())).thenReturn(respuesta());

        mvc.perform(patch("/api/solicitudes-venta/1/estado")
                        .header("Authorization", comprador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONTACTADO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/solicitudes-venta/1/estado")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONTACTADO\"}"))
                .andExpect(status().isOk());
    }
}
