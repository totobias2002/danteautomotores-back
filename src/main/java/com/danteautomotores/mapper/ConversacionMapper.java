package com.danteautomotores.mapper;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.UsuarioDeConversacionResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class ConversacionMapper {

    static final int LARGO_EXTRACTO = 120;

    private ConversacionMapper() {
    }

    // Necesita una transacción abierta: el resumen del auto recorre agencia y fotos (open-in-view: false).
    public static ConversacionResumenResponse toResumen(Conversacion conversacion, Mensaje ultimoMensaje) {
        return toResumen(conversacion, ultimoMensaje, 0);
    }

    public static ConversacionResumenResponse toResumen(Conversacion conversacion, Mensaje ultimoMensaje, long noLeidos) {
        return ConversacionResumenResponse.builder()
                .noLeidos(noLeidos)
                .id(conversacion.getId())
                .tipo(conversacion.getTipo())
                .estado(conversacion.getEstado())
                .publicacion(conversacion.getPublicacion() == null
                        ? null
                        : PublicacionMapper.toResumen(conversacion.getPublicacion()))
                .creadaEn(aInstante(conversacion.getCreadaEn()))
                .ultimoMensajeEn(aInstante(conversacion.getUltimoMensajeEn()))
                .ultimoMensaje(ultimoMensaje == null ? null : extracto(ultimoMensaje.getTexto()))
                .ultimoMensajeAutor(ultimoMensaje == null ? null : ultimoMensaje.getAutorTipo())
                .build();
    }

    /**
     * Fila de la bandeja del admin: el resumen más quién es el usuario (sin DNI ni teléfono, D-11). {@code noLeidos}
     * son los mensajes del usuario que la agencia todavía no leyó.
     */
    public static ConversacionResumenResponse toResumenParaAdmin(Conversacion conversacion, Mensaje ultimoMensaje, long noLeidos) {
        ConversacionResumenResponse resumen = toResumen(conversacion, ultimoMensaje, noLeidos);
        Usuario usuario = conversacion.getUsuario();
        resumen.setUsuario(UsuarioDeConversacionResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .build());
        return resumen;
    }

    /**
     * El hilo visto desde la agencia: con el usuario, y con los no leídos del lado de la agencia (los mensajes del usuario
     * que todavía no abrió), calculados del mismo hilo ya cargado.
     */
    public static ConversacionDetalleResponse toDetalleParaAdmin(Conversacion conversacion, List<Mensaje> mensajes) {
        Mensaje ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
        long noLeidos = mensajes.stream()
                .filter(m -> m.getAutorTipo() == AutorMensaje.USUARIO && m.getLeidoEn() == null)
                .count();
        return ConversacionDetalleResponse.builder()
                .conversacion(toResumenParaAdmin(conversacion, ultimo, noLeidos))
                .mensajes(mensajes.stream().map(ConversacionMapper::toMensaje).toList())
                .build();
    }

    public static MensajeResponse toMensaje(Mensaje mensaje) {
        return MensajeResponse.builder()
                .id(mensaje.getId())
                .autor(mensaje.getAutorTipo())
                .texto(mensaje.getTexto())
                .creadoEn(aInstante(mensaje.getCreadoEn()))
                .leido(mensaje.getLeidoEn() != null)
                .build();
    }

    // Los mensajes llegan ya ordenados; el resumen usa el último como extracto.
    public static ConversacionDetalleResponse toDetalle(Conversacion conversacion, List<Mensaje> mensajes) {
        Mensaje ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
        // Los no leídos salen del mismo hilo ya cargado: visto desde el comprador son los de la agencia sin abrir.
        long noLeidos = mensajes.stream()
                .filter(m -> m.getAutorTipo() == AutorMensaje.AGENCIA && m.getLeidoEn() == null)
                .count();
        return ConversacionDetalleResponse.builder()
                .conversacion(toResumen(conversacion, ultimo, noLeidos))
                .mensajes(mensajes.stream().map(ConversacionMapper::toMensaje).toList())
                .build();
    }

    // Las fechas se guardan en UTC (D-13): se devuelven como instante para que el navegador las muestre en su hora local.
    static Instant aInstante(LocalDateTime fecha) {
        return fecha == null ? null : fecha.toInstant(ZoneOffset.UTC);
    }

    static String extracto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        if (limpio.length() <= LARGO_EXTRACTO) {
            return limpio;
        }
        return limpio.substring(0, LARGO_EXTRACTO - 3).stripTrailing() + "...";
    }
}
