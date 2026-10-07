package com.danteautomotores.dto.conversacion;

import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

/**
 * Fila de Mis mensajes. Sin DNI, teléfono ni nada del usuario (D-11). Las fechas son instantes (ISO con Z, D-13).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionResumenResponse {
    private Long id;
    private TipoConversacion tipo;
    private EstadoConversacion estado;
    // Nulo si la conversación no tiene auto (cotización).
    private PublicacionResumenResponse publicacion;
    private Instant creadaEn;
    private Instant ultimoMensajeEn;
    // Fuera del toString: el texto de un mensaje y los datos del usuario no deben llegar a un log (D-11).
    @ToString.Exclude
    private String ultimoMensaje;
    private AutorMensaje ultimoMensajeAutor;
    // Mensajes de la agencia que el comprador todavía no abrió (D-06).
    private long noLeidos;
    // Quién es el usuario: solo lo llenan las respuestas de la bandeja del admin (D-05); al comprador no le viaja.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ToString.Exclude
    private UsuarioDeConversacionResponse usuario;
}
