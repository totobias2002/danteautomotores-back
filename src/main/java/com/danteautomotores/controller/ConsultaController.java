package com.danteautomotores.controller;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.dto.consulta.ConsultaResponse;
import com.danteautomotores.service.ConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping
    public ResponseEntity<ConsultaResponse> crear(@Valid @RequestBody ConsultaRequest request) {
        return ResponseEntity.ok(consultaService.crear(request));
    }

    @GetMapping("/publicacion/{publicacionId}")
    public ResponseEntity<List<ConsultaResponse>> listarPorPublicacion(@PathVariable Long publicacionId) {
        return ResponseEntity.ok(consultaService.listarPorPublicacion(publicacionId));
    }
}
