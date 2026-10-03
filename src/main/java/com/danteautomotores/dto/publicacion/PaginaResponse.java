package com.danteautomotores.dto.publicacion;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Contrato propio de página (no se serializa el Page de Spring Data, cuyo JSON no es estable). La página es base 1.
 */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
