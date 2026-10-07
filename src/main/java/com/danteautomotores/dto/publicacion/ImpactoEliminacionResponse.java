package com.danteautomotores.dto.publicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Solo cantidades: el aviso previo al borrado no debe exponer datos personales de los compradores.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactoEliminacionResponse {
    private long cantidadConversaciones;
    private long cantidadFavoritos;
}
