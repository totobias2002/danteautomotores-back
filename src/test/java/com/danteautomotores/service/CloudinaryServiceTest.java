package com.danteautomotores.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.danteautomotores.exception.ServicioExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    private static final String MENSAJE_SUBIDA = "No se pudo subir la imagen. Intentá de nuevo en unos minutos.";

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService servicio;

    private final MockMultipartFile archivo =
            new MockMultipartFile("archivo", "auto.jpg", "image/jpeg", new byte[]{1, 2, 3});

    @BeforeEach
    void armarServicio() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        servicio = new CloudinaryService(cloudinary);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void subirDevuelveLaUrlSeguraYElPublicIdTalCualLosDevuelveCloudinary() throws IOException {
        when(uploader.upload(any(), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/danteautomotores/publicaciones/abc.jpg",
                "public_id", "danteautomotores/publicaciones/abc"));

        CloudinaryService.ImagenSubida subida = servicio.subir(archivo);

        assertThat(subida.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/danteautomotores/publicaciones/abc.jpg");
        assertThat(subida.publicId()).isEqualTo("danteautomotores/publicaciones/abc");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void subirMandaCarpetaTipoDeRecursoYFormatosPermitidos() throws IOException {
        when(uploader.upload(any(), any(Map.class))).thenReturn(Map.of("secure_url", "u", "public_id", "p"));

        servicio.subir(archivo);

        ArgumentCaptor<Map> opciones = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), opciones.capture());
        assertThat(opciones.getValue())
                .containsEntry("folder", "danteautomotores/publicaciones")
                .containsEntry("resource_type", "image")
                .containsEntry("allowed_formats", "jpg,png,webp");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void subirEnvuelveUnaIOExceptionEnServicioExterno() throws IOException {
        when(uploader.upload(any(), any(Map.class))).thenThrow(new IOException("conexión caída"));

        assertThatThrownBy(() -> servicio.subir(archivo))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessage(MENSAJE_SUBIDA)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void subirEnvuelveUnaRuntimeExceptionDelSdkSinFiltrarSuTexto() throws IOException {
        when(uploader.upload(any(), any(Map.class))).thenThrow(new IllegalArgumentException("Must supply api_key"));

        assertThatThrownBy(() -> servicio.subir(archivo))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessage(MENSAJE_SUBIDA)
                .hasMessageNotContaining("api_key");
    }

    @Test
    void eliminarConPublicIdNuloOEnBlancoNoLlamaACloudinary() throws IOException {
        servicio.eliminar(null);
        servicio.eliminar("");
        servicio.eliminar("   ");

        verify(uploader, never()).destroy(anyString(), any(Map.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void eliminarLlamaADestroyConInvalidate() throws IOException {
        when(uploader.destroy(eq("pid"), any(Map.class))).thenReturn(Map.of("result", "ok"));

        servicio.eliminar("pid");

        ArgumentCaptor<Map> opciones = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq("pid"), opciones.capture());
        assertThat(opciones.getValue()).containsEntry("invalidate", true);
    }

    @Test
    void eliminarNoPropagaLaExcepcionSiDestroyFalla() throws IOException {
        when(uploader.destroy(eq("pid"), any(Map.class))).thenThrow(new IOException("timeout"));

        assertThatCode(() -> servicio.eliminar("pid")).doesNotThrowAnyException();
    }

    @Test
    void eliminarNoPropagaLaExcepcionSiElSdkTiraUnaRuntimeException() throws IOException {
        when(uploader.destroy(eq("pid"), any(Map.class))).thenThrow(new IllegalStateException("sin credenciales"));

        assertThatCode(() -> servicio.eliminar("pid")).doesNotThrowAnyException();
    }

    @Test
    void eliminarNoLanzaSiCloudinaryRespondeUnResultadoInesperado() throws IOException {
        when(uploader.destroy(eq("pid"), any(Map.class))).thenReturn(Map.of("result", "error"));

        assertThatCode(() -> servicio.eliminar("pid")).doesNotThrowAnyException();
    }

    @Test
    void eliminarAceptaNotFoundSinProblema() throws IOException {
        when(uploader.destroy(eq("pid"), any(Map.class))).thenReturn(Map.of("result", "not found"));

        assertThatCode(() -> servicio.eliminar("pid")).doesNotThrowAnyException();
    }
}
