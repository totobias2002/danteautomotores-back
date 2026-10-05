package com.danteautomotores.support;

import com.danteautomotores.mail.EmailSender;
import com.danteautomotores.mail.MensajeEmail;

import java.util.ArrayList;
import java.util.List;

/**
 * EmailSender de captura para los tests: guarda los mails en memoria para verificarlos sin Spring ni red.
 * Sincronizado porque los envíos reales corren en el hilo del ejecutor asíncrono.
 */
public class EmailSenderEnMemoria implements EmailSender {

    private final List<MensajeEmail> enviados = new ArrayList<>();

    @Override
    public synchronized void enviar(MensajeEmail mensaje) {
        enviados.add(mensaje);
    }

    /** Copia de los mails enviados, en orden. */
    public synchronized List<MensajeEmail> enviados() {
        return List.copyOf(enviados);
    }

    /** El último mail enviado, o null si no se mandó ninguno. */
    public synchronized MensajeEmail ultimo() {
        return enviados.isEmpty() ? null : enviados.get(enviados.size() - 1);
    }

    public synchronized void limpiar() {
        enviados.clear();
    }
}
