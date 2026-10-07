package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.ImpactoEliminacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.service.PublicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/publicaciones")
@RequiredArgsConstructor
public class AdminPublicacionController {

    private final PublicacionService publicacionService;

    @GetMapping
    public ResponseEntity<List<PublicacionResponse>> listar() {
        return ResponseEntity.ok(publicacionService.listarParaAdmin());
    }

    // Cuántas conversaciones y favoritos se borran junto con el auto; el panel lo muestra antes de confirmar.
    @GetMapping("/{id}/impacto-eliminacion")
    public ResponseEntity<ImpactoEliminacionResponse> impactoEliminacion(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.obtenerImpactoEliminacion(id));
    }
}
