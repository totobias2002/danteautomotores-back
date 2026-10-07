package com.danteautomotores.dto.conversacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** El hilo completo de una conversación: sus datos y los mensajes en orden ascendente. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionDetalleResponse {
    private ConversacionResumenResponse conversacion;
    private List<MensajeResponse> mensajes;
}
