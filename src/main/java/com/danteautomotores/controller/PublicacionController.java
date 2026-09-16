package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;

    @GetMapping
    public ResponseEntity<List<PublicacionResponse>> buscar(
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) Integer anioMin,
            @RequestParam(required = false) Integer anioMax,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) EstadoPublicacion estado,
            @RequestParam(required = false) Long agenciaId
    ) {
        return ResponseEntity.ok(publicacionService.buscar(marca, modelo, anioMin, anioMax, precioMin, precioMax, estado, agenciaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(publicacionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PublicacionResponse> crear(@Valid @RequestBody PublicacionRequest request) {
        return ResponseEntity.ok(publicacionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PublicacionRequest request) {
        return ResponseEntity.ok(publicacionService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PublicacionResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(publicacionService.cambiarEstado(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        publicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fotos")
    public ResponseEntity<PublicacionResponse> agregarFoto(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(publicacionService.agregarFoto(id, archivo));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        publicacionService.eliminarFoto(id, fotoId);
        return ResponseEntity.noContent().build();
    }
}
