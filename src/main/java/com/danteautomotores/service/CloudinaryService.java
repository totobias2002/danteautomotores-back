package com.danteautomotores.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.danteautomotores.exception.ServicioExternoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /** Url segura y public_id tal cual los devuelve Cloudinary (el public_id sirve después para borrar la foto). */
    public record ImagenSubida(String url, String publicId) {}

    public ImagenSubida subir(MultipartFile archivo) {
        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "danteautomotores/publicaciones",
                            "resource_type", "image",
                            "allowed_formats", "jpg,png,webp"));
            return new ImagenSubida((String) resultado.get("secure_url"), (String) resultado.get("public_id"));
        } catch (IOException | RuntimeException e) {
            // El detalle del SDK (por ejemplo "Must supply api_key") queda en la causa, no llega al cliente.
            throw new ServicioExternoException("No se pudo subir la imagen. Intentá de nuevo en unos minutos.", e);
        }
    }

    // Temporal: PublicacionService todavía lo usa; la tarea siguiente lo reemplaza por subir() y lo elimina.
    public String subirImagen(MultipartFile archivo) {
        return subir(archivo).url();
    }

    /** Borrado best-effort: si Cloudinary falla no se bloquea la operación del admin, solo se deja un aviso en el log. */
    public void eliminar(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Map<?, ?> resultado = cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("invalidate", true, "resource_type", "image"));
            Object estado = resultado == null ? null : resultado.get("result");
            if (!"ok".equals(estado) && !"not found".equals(estado)) {
                log.warn("Cloudinary no confirmó el borrado de {}: result={}", publicId, estado);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo borrar {} de Cloudinary", publicId, e);
        }
    }
}
