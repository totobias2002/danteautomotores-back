package com.danteautomotores.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenValidatorTest {

    // Firmas reales: JPEG (FF D8 FF), PNG (89 'PNG' 0D 0A 1A 0A) y WebP ('RIFF' + 4 bytes de largo + 'WEBP').
    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
    };
    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
            'I', 'H', 'D', 'R', 0x00, 0x00, 0x00, 0x01
    };
    private static final byte[] WEBP = {
            'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
            'V', 'P', '8', ' ', 0x18, 0x00, 0x00, 0x00
    };
    // Un ejecutable de Windows ("MZ") que se hace pasar por imagen.
    private static final byte[] EJECUTABLE = {
            'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xFF, 0x00, 0x00
    };

    private final ImagenValidator validador = new ImagenValidator();

    private static MockMultipartFile archivo(String contentType, byte[] contenido) {
        return new MockMultipartFile("archivo", "foto", contentType, contenido);
    }

    @Test
    void aceptaUnJpegReal() {
        assertThatCode(() -> validador.validar(archivo("image/jpeg", JPEG))).doesNotThrowAnyException();
    }

    @Test
    void aceptaUnPngReal() {
        assertThatCode(() -> validador.validar(archivo("image/png", PNG))).doesNotThrowAnyException();
    }

    @Test
    void aceptaUnWebpReal() {
        assertThatCode(() -> validador.validar(archivo("image/webp", WEBP))).doesNotThrowAnyException();
    }

    @Test
    void elContentTypeSeComparaSinImportarMayusculas() {
        assertThatCode(() -> validador.validar(archivo("IMAGE/JPEG", JPEG))).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnArchivoVacio() {
        assertThatThrownBy(() -> validador.validar(archivo("image/jpeg", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Elegí una imagen para subir");
    }

    @Test
    void rechazaUnArchivoNulo() {
        assertThatThrownBy(() -> validador.validar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Elegí una imagen para subir");
    }

    @Test
    void rechazaUnaImagenDeMasDe10Mb() {
        byte[] enorme = new byte[(int) ImagenValidator.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, enorme, 0, JPEG.length);

        assertThatThrownBy(() -> validador.validar(archivo("image/jpeg", enorme)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La imagen supera el máximo de 10 MB");
    }

    @Test
    void aceptaUnaImagenDeExactamente10Mb() {
        byte[] justa = new byte[(int) ImagenValidator.MAX_BYTES];
        System.arraycopy(JPEG, 0, justa, 0, JPEG.length);

        assertThatCode(() -> validador.validar(archivo("image/jpeg", justa))).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnGifPorContentType() {
        assertThatThrownBy(() -> validador.validar(archivo("image/gif", JPEG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Usá JPG, PNG o WebP");
    }

    @Test
    void rechazaUnHeicPorContentType() {
        assertThatThrownBy(() -> validador.validar(archivo("image/heic", JPEG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Usá JPG, PNG o WebP");
    }

    @Test
    void rechazaUnPdfPorContentType() {
        assertThatThrownBy(() -> validador.validar(archivo("application/pdf", JPEG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Usá JPG, PNG o WebP");
    }

    @Test
    void rechazaUnArchivoSinContentType() {
        assertThatThrownBy(() -> validador.validar(archivo(null, JPEG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Usá JPG, PNG o WebP");
    }

    @Test
    void rechazaUnEjecutableQueDeclaraSerJpeg() {
        assertThatThrownBy(() -> validador.validar(archivo("image/jpeg", EJECUTABLE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no es una imagen válida (JPG, PNG o WebP)");
    }

    @Test
    void rechazaUnRiffQueNoEsWebp() {
        byte[] wav = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' '};

        assertThatThrownBy(() -> validador.validar(archivo("image/webp", wav)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no es una imagen válida (JPG, PNG o WebP)");
    }

    @Test
    void rechazaUnArchivoMasCortoQueLaFirma() {
        assertThatThrownBy(() -> validador.validar(archivo("image/png", new byte[]{(byte) 0x89, 'P'})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no es una imagen válida (JPG, PNG o WebP)");
    }
}
