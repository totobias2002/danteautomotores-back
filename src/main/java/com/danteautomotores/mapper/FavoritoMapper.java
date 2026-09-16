package com.danteautomotores.mapper;

import com.danteautomotores.dto.favorito.FavoritoResponse;
import com.danteautomotores.entity.Favorito;

public class FavoritoMapper {

    private FavoritoMapper() {
    }

    public static FavoritoResponse toResponse(Favorito favorito) {
        return FavoritoResponse.builder()
                .id(favorito.getId())
                .publicacion(PublicacionMapper.toResponse(favorito.getPublicacion()))
                .fecha(favorito.getFecha())
                .build();
    }
}
