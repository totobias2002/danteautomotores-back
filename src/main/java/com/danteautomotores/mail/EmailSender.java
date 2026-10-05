package com.danteautomotores.mail;

import com.danteautomotores.exception.ServicioExternoException;

/**
 * Servicio de mail propio del proyecto: quien necesita mandar un mail depende de esta interfaz y no del proveedor,
 * de modo que cambiar de Brevo a otro no toca a quien envía (D-12). El envío es síncrono; los servicios que no
 * pueden esperar lo llaman desde el ejecutor asíncrono {@code mailExecutor}.
 */
public interface EmailSender {

    /**
     * Manda el mail.
     *
     * @throws ServicioExternoException si el proveedor rechaza el envío, no responde a tiempo o no se lo puede
     *                                  alcanzar; el mensaje es apto para el usuario final y nunca lleva el
     *                                  contenido del mail.
     */
    void enviar(MensajeEmail mensaje);
}
