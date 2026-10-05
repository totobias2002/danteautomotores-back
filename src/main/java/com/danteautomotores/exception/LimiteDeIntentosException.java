package com.danteautomotores.exception;

/**
 * Se excedió el límite de intentos de una acción sensible (login, reenvío de mails, recuperación de contraseña).
 * Se responde como 429 con el header Retry-After. El mensaje ya está redactado para el usuario y no depende de la
 * cuenta consultada.
 */
public class LimiteDeIntentosException extends RuntimeException {

    public static final long ESPERA_POR_DEFECTO_SEGUNDOS = 60;

    private final long reintentarEnSegundos;

    public LimiteDeIntentosException(String mensaje) {
        this(mensaje, ESPERA_POR_DEFECTO_SEGUNDOS);
    }

    public LimiteDeIntentosException(String mensaje, long reintentarEnSegundos) {
        super(mensaje);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
