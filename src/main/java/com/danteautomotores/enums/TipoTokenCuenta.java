package com.danteautomotores.enums;

/**
 * Para qué sirve un token de cuenta. Se guarda como texto sin CHECK en la base: sumar un tipo no exige otra migración.
 */
public enum TipoTokenCuenta {
    CONFIRMAR_EMAIL,
    RESTABLECER_CONTRASENA
}
