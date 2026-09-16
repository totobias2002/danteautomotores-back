package com.danteautomotores.dto.favorito;

import com.danteautomotores.dto.publicacion.PublicacionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoritoResponse {
    private Long id;
    private PublicacionResponse publicacion;
    private LocalDateTime fecha;
}
