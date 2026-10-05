package com.danteautomotores.exception;

import com.danteautomotores.enums.DatoFaltante;

import java.util.List;

/**
 * La cuenta autenticada todavía no está verificada (D-01) y la acción exige que lo esté. Se responde como 403 con
 * codigo CUENTA_NO_VERIFICADA y la lista de datos faltantes, para que el front lleve al usuario a completarlos.
 */
public class CuentaNoVerificadaException extends RuntimeException {

    public static final String CODIGO = "CUENTA_NO_VERIFICADA";

    private final List<DatoFaltante> faltantes;

    public CuentaNoVerificadaException(List<DatoFaltante> faltantes) {
        super("Cuenta no verificada");
        this.faltantes = List.copyOf(faltantes);
    }

    public List<DatoFaltante> getFaltantes() {
        return faltantes;
    }
}
