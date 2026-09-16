package com.danteautomotores.controller;

import com.danteautomotores.dto.agencia.AgenciaRequest;
import com.danteautomotores.dto.agencia.AgenciaResponse;
import com.danteautomotores.service.AgenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agencias")
@RequiredArgsConstructor
public class AgenciaController {

    private final AgenciaService agenciaService;

    @GetMapping
    public ResponseEntity<List<AgenciaResponse>> listar() {
        return ResponseEntity.ok(agenciaService.listar());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<AgenciaResponse> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(agenciaService.obtenerPorSlug(slug));
    }

    @PostMapping
    public ResponseEntity<AgenciaResponse> crear(@Valid @RequestBody AgenciaRequest request) {
        return ResponseEntity.ok(agenciaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgenciaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AgenciaRequest request) {
        return ResponseEntity.ok(agenciaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        agenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
