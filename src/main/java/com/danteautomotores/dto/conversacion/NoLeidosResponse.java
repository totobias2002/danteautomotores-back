package com.danteautomotores.dto.conversacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuántos mensajes sin leer tiene quien pregunta: el comprador cuenta los de la agencia en sus conversaciones y el
 * admin los de los usuarios en toda la bandeja (D-06). Solo números: sin ids ni texto (T-04-15).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoLeidosResponse {
    // Cantidad de mensajes sin leer.
    private long noLeidos;
    // Cantidad de conversaciones con al menos un mensaje sin leer.
    private long conversaciones;
}
