package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.security.SecurityUtils;
import com.danteautomotores.service.ConversacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lado del comprador. Siempre trabaja con la cuenta del token: ningún endpoint recibe el id de un usuario (T-04-01). */
@RestController
@RequestMapping("/api/conversaciones")
@RequiredArgsConstructor
public class ConversacionController {

    private final ConversacionService conversacionService;

    @PostMapping
    public ResponseEntity<ConversacionResumenResponse> iniciarCompra(@Valid @RequestBody ConversacionRequest request) {
        return ResponseEntity.ok(conversacionService.iniciarCompra(request, SecurityUtils.obtenerEmailAutenticado()));
    }

    @GetMapping
    public ResponseEntity<List<ConversacionResumenResponse>> listarMias() {
        return ResponseEntity.ok(conversacionService.listarMias(SecurityUtils.obtenerEmailAutenticado()));
    }
}
