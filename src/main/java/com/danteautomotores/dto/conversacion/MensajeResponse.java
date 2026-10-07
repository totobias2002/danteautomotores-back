package com.danteautomotores.dto.conversacion;

import com.danteautomotores.enums.AutorMensaje;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

/** Un mensaje del hilo. Sin datos de la cuenta que lo escribió: del lado de la agencia es siempre "Dante Automotores" (D-05). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {
    private Long id;
    private AutorMensaje autor;
    // Fuera del toString: el texto de un mensaje es privado y no debe llegar a un log (D-11).
    @ToString.Exclude
    private String texto;
    private Instant creadoEn;
    private boolean leido;
}
