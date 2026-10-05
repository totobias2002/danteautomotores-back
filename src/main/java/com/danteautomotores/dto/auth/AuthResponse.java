package com.danteautomotores.dto.auth;

import com.danteautomotores.enums.DatoFaltante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de registro y login. Los campos de cuenta son aditivos (un front viejo los ignora).
 * Nunca lleva el DNI ni el teléfono: solo qué datos faltan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private boolean emailConfirmado;
    private boolean cuentaVerificada;
    private List<DatoFaltante> faltantes;
}
