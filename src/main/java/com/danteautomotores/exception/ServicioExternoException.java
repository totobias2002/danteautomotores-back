package com.danteautomotores.exception;

/**
 * Falla de un servicio externo (por ejemplo Cloudinary). El mensaje es apto para mostrarle al usuario final;
 * el detalle técnico va en la causa y queda solo en el log del servidor. La API la responde como 502.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
