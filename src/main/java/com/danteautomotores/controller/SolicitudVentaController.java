package com.danteautomotores.controller;

import com.danteautomotores.dto.solicitudventa.CambiarEstadoSolicitudRequest;
import com.danteautomotores.dto.solicitudventa.SolicitudVentaRequest;
import com.danteautomotores.dto.solicitudventa.SolicitudVentaResponse;
import com.danteautomotores.service.SolicitudVentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-venta")
@RequiredArgsConstructor
public class SolicitudVentaController {

    private final SolicitudVentaService solicitudVentaService;

    @PostMapping
    public ResponseEntity<SolicitudVentaResponse> crear(@Valid @RequestBody SolicitudVentaRequest request) {
        return ResponseEntity.ok(solicitudVentaService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudVentaResponse>> listarTodas() {
        return ResponseEntity.ok(solicitudVentaService.listarTodas());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<SolicitudVentaResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoSolicitudRequest request) {
        return ResponseEntity.ok(solicitudVentaService.cambiarEstado(id, request));
    }
}
