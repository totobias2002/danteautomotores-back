package com.danteautomotores.controller;

import com.danteautomotores.dto.publicacion.CambiarDestacadoRequest;
import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.dto.publicacion.ReordenarFotosRequest;
import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.service.CatalogoService;
import com.danteautomotores.service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;
import java.util.List;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final CatalogoService catalogoService;

    // Público (sin token): catálogo paginado de a 24, con filtros y orden resueltos en el servidor.
    @GetMapping
    public ResponseEntity<PaginaResponse<PublicacionResumenResponse>> buscar(@ModelAttribute FiltrosCatalogo filtros) {
        return ResponseEntity.ok(catalogoService.buscar(filtros));
    }

    // Un número de página ilegible (pagina=abc, o fuera de rango) cae a la primera página en vez de dar 400.
    @InitBinder("filtrosCatalogo")
    void configurarBinding(WebDataBinder binder) {
        binder.registerCustomEditor(Integer.class, "pagina", new PropertyEditorSupport() {
            @Override
            public void setAsText(String texto) {
                try {
                    setValue(texto == null || texto.isBlank() ? null : Integer.valueOf(texto.trim()));
                } catch (NumberFormatException e) {
                    setValue(null);
                }
            }
        });
    }

    // Público (sin token). La ruta literal gana a /{id} por especificidad.
    @GetMapping("/destacados")
    public ResponseEntity<List<PublicacionResumenResponse>> destacados(@RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(catalogoService.destacados(limite));
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

    @PatchMapping("/{id}/destacado")
    public ResponseEntity<PublicacionResponse> cambiarDestacado(@PathVariable Long id, @Valid @RequestBody CambiarDestacadoRequest request) {
        return ResponseEntity.ok(publicacionService.cambiarDestacado(id, request));
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

    @PutMapping("/{id}/fotos/orden")
    public ResponseEntity<PublicacionResponse> reordenarFotos(@PathVariable Long id, @Valid @RequestBody ReordenarFotosRequest request) {
        return ResponseEntity.ok(publicacionService.reordenarFotos(id, request));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> eliminarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        publicacionService.eliminarFoto(id, fotoId);
        return ResponseEntity.noContent().build();
    }
}
