package com.danteautomotores.controller;

import com.danteautomotores.dto.favorito.FavoritoResponse;
import com.danteautomotores.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @GetMapping
    public ResponseEntity<List<FavoritoResponse>> listar() {
        return ResponseEntity.ok(favoritoService.listar());
    }

    @PostMapping("/{publicacionId}")
    public ResponseEntity<FavoritoResponse> agregar(@PathVariable Long publicacionId) {
        return ResponseEntity.ok(favoritoService.agregar(publicacionId));
    }

    @DeleteMapping("/{publicacionId}")
    public ResponseEntity<Void> quitar(@PathVariable Long publicacionId) {
        favoritoService.quitar(publicacionId);
        return ResponseEntity.noContent().build();
    }
}
