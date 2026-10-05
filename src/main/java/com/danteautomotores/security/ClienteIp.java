package com.danteautomotores.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP del cliente de mejor esfuerzo: el primer valor de {@code X-Forwarded-For} (lo que el proxy de Railway informa) y,
 * si falta, la dirección remota de la conexión.
 *
 * <p>No es una fuente confiable: detrás del proxy el header puede venir falsificado por el cliente, y varias personas
 * pueden compartir una IP. Por eso la IP solo sirve como una capa extra de los límites de intentos y nunca para nada
 * crítico; la defensa real es el límite por cuenta (mail). Nunca se loguea.
 */
public final class ClienteIp {

    private static final String HEADER = "X-Forwarded-For";
    // Una IP válida (IPv6 incluida) entra en 45 caracteres: el tope evita que un header gigante infle las claves del limitador.
    private static final int LARGO_MAXIMO = 64;

    private ClienteIp() {
    }

    public static String de(HttpServletRequest request) {
        String reenviada = request.getHeader(HEADER);
        if (reenviada != null) {
            String primera = reenviada.split(",", 2)[0].trim();
            if (!primera.isEmpty()) {
                return recortar(primera);
            }
        }
        String remota = request.getRemoteAddr();
        return remota == null ? "desconocida" : recortar(remota.trim());
    }

    private static String recortar(String valor) {
        return valor.length() > LARGO_MAXIMO ? valor.substring(0, LARGO_MAXIMO) : valor;
    }
}
