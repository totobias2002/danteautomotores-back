package com.danteautomotores.controller;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.service.ConversacionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja de la agencia. /api/admin/** ya es solo de ADMIN en SecurityConfig (comprador 403, sin token 401). */
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
}
