package com.danteautomotores.service;

import com.danteautomotores.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Valida que lo que se sube como foto sea realmente una imagen JPEG, PNG o WebP. El Content-Type lo manda el
 * cliente y se falsifica con facilidad, así que además de compararlo con una lista permitida se verifica la
 * firma binaria (magic bytes) del archivo.
 */
@Component
public class ImagenValidator {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final int MAX_FOTOS = 10;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    public void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("Elegí una imagen para subir");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ReglaDeNegocioException("La imagen supera el máximo de 10 MB");
        }

        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new ReglaDeNegocioException("Formato no permitido. Usá JPG, PNG o WebP");
        }

        byte[] cabecera = leerCabecera(archivo);
        if (!esJpeg(cabecera) && !esPng(cabecera) && !esWebp(cabecera)) {
            throw new ReglaDeNegocioException("El archivo no es una imagen válida (JPG, PNG o WebP)");
        }
    }

    private static byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(12); // alcanza para las firmas de JPEG, PNG y WebP
        } catch (IOException e) {
            throw new ReglaDeNegocioException("No se pudo leer el archivo");
        }
    }

    private static boolean esJpeg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean esPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    private static boolean esWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }
}
