package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.exception.ReglaDeNegocioException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reglas compartidas por el restablecimiento y el cambio de contraseña: el tope de 72 bytes de BCrypt y el instante
 * del cambio que cierra las demás sesiones (claim pca, D-19).
 */
final class ContrasenaCuenta {

    static final int MAX_BYTES = 72;

    private ContrasenaCuenta() {
    }

    /** BCrypt solo mira los primeros 72 bytes: más que eso se truncaría en silencio (acentos y símbolos ocupan más de 1). */
    static void verificarLargo(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ReglaDeNegocioException(
                    "La contraseña es demasiado larga: no puede superar los 72 bytes (los acentos y símbolos ocupan más de uno).");
        }
    }

    /**
     * Fija {@code passwordCambiadaEn} con el reloj del proyecto sin retroceder nunca y siempre en un segundo posterior
     * al valor anterior: el claim pca se compara en segundos, así que un cambio dentro del mismo segundo (o un reloj
     * que retrocede por el horario de verano) dejaría vivas las sesiones anteriores.
     */
    static void marcarCambio(Usuario usuario, Clock clock) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime anterior = usuario.getPasswordCambiadaEn();
        if (anterior != null
                && !ahora.truncatedTo(ChronoUnit.SECONDS).isAfter(anterior.truncatedTo(ChronoUnit.SECONDS))) {
            ahora = anterior.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        }
        usuario.setPasswordCambiadaEn(ahora);
    }

    /**
     * Ejecuta la acción cuando la transacción en curso confirma (o ya, si no hay transacción). Sirve para que el mail
     * salga recién cuando el token y el cambio están guardados, y no salga si la transacción se revierte.
     */
    static void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
