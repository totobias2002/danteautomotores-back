package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.security.SecurityUtils;
import com.danteautomotores.service.ConversacionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja y hilo de la agencia. /api/admin/** ya es solo de ADMIN en SecurityConfig (comprador 403, sin token 401). */
@RestController
@RequestMapping("/api/admin/conversaciones")
@RequiredArgsConstructor
public class AdminConversacionController {

    private final ConversacionAdminService conversacionAdminService;

    @GetMapping
    public ResponseEntity<PaginaResponse<ConversacionResumenResponse>> listar(
            @RequestParam(required = false) TipoConversacion tipo,
            @RequestParam(required = false) EstadoConversacion estado,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "1") int pagina) {
        return ResponseEntity.ok(conversacionAdminService.listar(tipo, estado, soloNoLeidas, pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversacionDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionAdminService.obtener(id));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<MensajeResponse> responder(@PathVariable Long id, @Valid @RequestBody MensajeRequest request) {
        return ResponseEntity.ok(conversacionAdminService.responder(id, request, SecurityUtils.obtenerEmailAutenticado()));
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<NoLeidosResponse> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionAdminService.marcarLeida(id));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<ConversacionResumenResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionAdminService.cerrar(id));
    }

    @PostMapping("/{id}/reabrir")
    public ResponseEntity<ConversacionResumenResponse> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(conversacionAdminService.reabrir(id));
    }
}
