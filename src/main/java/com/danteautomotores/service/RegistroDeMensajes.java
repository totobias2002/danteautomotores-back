package com.danteautomotores.service;

import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Único lugar donde se registra un mensaje: los avisos por mail se enganchan acá y no en cada servicio. Exige una
 * transacción abierta, así el mensaje y el movimiento de la conversación se guardan juntos o no se guardan.
 */
@Component
@RequiredArgsConstructor
public class RegistroDeMensajes {

    private final MensajeRepository mensajeRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public Mensaje agregar(Conversacion conversacion, Usuario autor, AutorMensaje autorTipo, String texto) {
        // UTC explícito (D-13): el reloj del proyecto usa la zona del servidor.
        LocalDateTime ahora = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        Mensaje mensaje = mensajeRepository.save(Mensaje.builder()
                .conversacion(conversacion)
                .autor(autor)
                .autorTipo(autorTipo)
                .texto(texto)
                .creadoEn(ahora)
                .build());
        conversacion.setUltimoMensajeEn(ahora);
        return mensaje;
    }
}
