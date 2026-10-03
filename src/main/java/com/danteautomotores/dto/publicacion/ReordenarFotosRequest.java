package com.danteautomotores.dto.publicacion;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReordenarFotosRequest {

    // Lista completa de ids de las fotos del auto, en el orden nuevo. La primera es la portada.
    @NotEmpty
    private List<Long> fotoIds;
}
