package com.danteautomotores.controller;

import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.security.SecurityUtils;
import com.danteautomotores.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil de la propia cuenta. Ningún endpoint recibe un id: la cuenta sale siempre del token, así que no hay forma
 * de pedir o modificar el perfil de otro (sin IDOR). Cae en anyRequest().authenticated(); nunca va bajo /api/auth/**,
 * que es público.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerPerfil() {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(SecurityUtils.obtenerEmailAutenticado()));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(SecurityUtils.obtenerEmailAutenticado(), request));
    }
}
