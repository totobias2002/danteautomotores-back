package com.danteautomotores.service;

import com.danteautomotores.mail.MensajeEmail;
import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas de los mails de cuenta, en español rioplatense y de estilo sobrio. Cada mail lleva texto plano y un HTML
 * simple (un párrafo, un botón-link y la URL completa debajo para quien no ve el botón). Clase de utilidades sin
 * estado: arma el {@link MensajeEmail} y no envía nada. Todo dato que entra al HTML se escapa.
 */
public final class PlantillasEmail {

    static final String ASUNTO_CONFIRMAR_EMAIL = "Confirmá tu mail en Dante Automotores";
    static final String ASUNTO_RESTABLECER_CONTRASENA = "Cambiá tu contraseña";
    static final String ASUNTO_CONTRASENA_CAMBIADA = "Tu contraseña fue cambiada";
    // Asuntos fijos: ningún dato escrito por el usuario entra al asunto (D-11).
    static final String ASUNTO_MENSAJE_NUEVO_USUARIO = "Tenés un mensaje nuevo en Dante Automotores";
    static final String ASUNTO_MENSAJE_NUEVO_AGENCIA = "Mensaje nuevo en la bandeja de Dante Automotores";

    private static final String FIRMA = "Dante Automotores";

    private PlantillasEmail() {
    }

    /** Confirmar el mail: el link vale 24 horas. */
    public static MensajeEmail confirmarEmail(String nombre, String email, String link) {
        String parrafo = "Para terminar de crear tu cuenta en Dante Automotores, confirmá tu mail. "
                + "El link vale 24 horas. Si no creaste una cuenta, ignorá este mail.";
        return armar(nombre, email, ASUNTO_CONFIRMAR_EMAIL, parrafo, "Confirmar mi mail", link);
    }

    /** Cambiar la contraseña: el link vale 1 hora y se usa una sola vez. */
    public static MensajeEmail restablecerContrasena(String nombre, String email, String link) {
        String parrafo = "Recibimos un pedido para cambiar la contraseña de tu cuenta en Dante Automotores. "
                + "El link vale 1 hora y se usa una sola vez. Si no pediste esto, ignorá este mail.";
        return armar(nombre, email, ASUNTO_RESTABLECER_CONTRASENA, parrafo, "Cambiar mi contraseña", link);
    }

    /** Aviso de contraseña cambiada: sin ningún link, para que no sirva de señuelo. */
    public static MensajeEmail contrasenaCambiada(String nombre, String email) {
        String parrafo = "La contraseña de tu cuenta en Dante Automotores fue cambiada. Si no fuiste vos, "
                + "entrá a la página de ingreso, elegí \"Olvidé mi contraseña\" para recuperar tu cuenta "
                + "y escribinos para avisarnos.";
        return armar(nombre, email, ASUNTO_CONTRASENA_CAMBIADA, parrafo, null, null);
    }

    /** Aviso al usuario de que la agencia le escribió. No lleva el texto del mensaje (D-11). */
    public static MensajeEmail mensajeNuevoParaUsuario(String nombre, String email, String descripcionDelAuto,
                                                       String link) {
        String parrafo = "La agencia te escribió sobre " + descripcionDelAuto + ". Entrá a Mis mensajes "
                + "para leer la respuesta y contestar.";
        return armar(nombre, email, ASUNTO_MENSAJE_NUEVO_USUARIO, parrafo, "Ver mi conversación", link);
    }

    /** Aviso a la agencia de que un usuario escribió. No lleva el texto del mensaje (D-11). */
    public static MensajeEmail mensajeNuevoParaLaAgencia(String nombreDelAdmin, String email, String nombreDelUsuario,
                                                         String descripcionDelAuto, String link) {
        String parrafo = nombreDelUsuario + " escribió sobre " + descripcionDelAuto + ". Abrí la bandeja "
                + "para leer el mensaje y responder.";
        return armar(nombreDelAdmin, email, ASUNTO_MENSAJE_NUEVO_AGENCIA, parrafo, "Abrir la conversación", link);
    }

    private static MensajeEmail armar(String nombre, String email, String asunto, String parrafo,
                                      String textoDelBoton, String link) {
        String saludo = "Hola " + nombre + ",";

        StringBuilder texto = new StringBuilder(saludo).append("\n\n").append(parrafo).append("\n");
        if (link != null) {
            texto.append("\n").append(link).append("\n");
        }
        texto.append("\n").append(FIRMA).append("\n");

        StringBuilder html = new StringBuilder()
                .append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:16px;color:#1f2937;")
                .append("max-width:560px;margin:0 auto;padding:24px;\">")
                .append("<p>").append(HtmlUtils.htmlEscape(saludo)).append("</p>")
                .append("<p>").append(HtmlUtils.htmlEscape(parrafo)).append("</p>");
        if (link != null) {
            String linkHtml = HtmlUtils.htmlEscape(link);
            html.append("<p><a href=\"").append(linkHtml).append("\" style=\"display:inline-block;")
                    .append("background:#0f2a43;color:#ffffff;text-decoration:none;padding:12px 24px;")
                    .append("border-radius:6px;\">").append(HtmlUtils.htmlEscape(textoDelBoton)).append("</a></p>")
                    .append("<p style=\"font-size:14px;color:#6b7280;\">Si el botón no funciona, copiá este link ")
                    .append("en tu navegador:<br>").append(linkHtml).append("</p>");
        }
        html.append("<p style=\"color:#6b7280;\">").append(FIRMA).append("</p></div>");

        return new MensajeEmail(email, nombre, asunto, html.toString(), texto.toString());
    }
}
