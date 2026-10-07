package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
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

    // El literal tiene prioridad sobre /{id}. Contesta a cualquier sesión: el comprador cuenta lo suyo y el admin la bandeja.
    @GetMapping("/no-leidas")
    public ResponseEntity<NoLeidosResponse> contarNoLeidos() {
        return ResponseEntity.ok(conversacionService.contarNoLeidos(SecurityUtils.obtenerEmailAutenticado()));
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<NoLeidosResponse> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionService.marcarLeida(id, SecurityUtils.obtenerEmailAutenticado()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversacionDetalleResponse> obtenerMia(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionService.obtenerMia(id, SecurityUtils.obtenerEmailAutenticado()));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<MensajeResponse> enviarMensaje(@PathVariable Long id, @Valid @RequestBody MensajeRequest request) {
        return ResponseEntity.ok(conversacionService.enviarMensaje(id, request, SecurityUtils.obtenerEmailAutenticado()));
    }
}
