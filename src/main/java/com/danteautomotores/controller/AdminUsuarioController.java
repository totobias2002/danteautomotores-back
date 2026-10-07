package com.danteautomotores.controller;

import com.danteautomotores.dto.usuario.UsuarioFichaResponse;
import com.danteautomotores.service.UsuarioAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ficha de un usuario para la agencia. /api/admin/** ya es solo de ADMIN en SecurityConfig (comprador 403, sin token 401). */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioFichaResponse> obtenerFicha(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioAdminService.obtenerFicha(id));
    }
}
