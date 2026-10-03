package com.danteautomotores.exception;

/**
 * Violación de una regla de negocio provocada por el pedido del cliente (por ejemplo "hasta 10 fotos por auto").
 * Se responde como 400 con su mensaje, que ya está redactado para el usuario. Un IllegalArgumentException a
 * secas (de Spring, Hibernate o el JDK) NO se mapea a 400: es un error del servidor y sale como 500 genérico.
 */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String message) {
        super(message);
    }
}
