package com.danteautomotores.security;

/**
 * Lo que el back toma de un ID token de Google ya verificado. No usa tipos de Spring Security para que el service
 * de cuentas no dependa de la librería de tokens.
 *
 * <p>La cuenta se identifica por {@code sub} (estable); el mail solo sirve para la unión inicial y solo si
 * {@code emailVerificado} es verdadero. {@code nombre} y {@code apellido} pueden ser nulos si Google no los envía.
 */
public record IdentidadGoogle(
        String sub,
        String email,
        boolean emailVerificado,
        String nombre,
        String apellido,
        String nombreCompleto) {
}
