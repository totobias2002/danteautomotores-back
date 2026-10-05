package com.danteautomotores.mail;

/**
 * Un mail transaccional listo para enviar. {@code html} y {@code texto} llevan el mismo contenido: el texto plano es
 * la versión de respaldo y la que ve {@link LogEmailSender} en desarrollo.
 */
public record MensajeEmail(String paraEmail, String paraNombre, String asunto, String html, String texto) {
}
