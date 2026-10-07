package com.danteautomotores.dto.conversacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Quién es el usuario de una conversación, visto por la agencia (D-05, D-11). Solo lo que hace falta para saber con
 * quién se habla: nunca DNI ni teléfono.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDeConversacionResponse {
    private Long id;
    private String nombre;
    private String apellido;
    @ToString.Exclude
    private String email;
}
